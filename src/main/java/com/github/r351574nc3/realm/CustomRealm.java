/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.r351574nc3.realm;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.xpack.security.user.User;
import org.elasticsearch.xpack.security.authc.AuthenticationToken;
import org.elasticsearch.xpack.security.authc.Realm;
import org.elasticsearch.xpack.security.authc.RealmConfig;
import org.elasticsearch.xpack.security.authc.support.UsernamePasswordToken;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.ClientCredentialsTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.github.r351574nc3.realm.userinfo.UserInfoRequest;
import com.github.r351574nc3.realm.userinfo.UserInfoResponse;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Custom Realm Implementation for Kibana that authenticates against an oauth2 source
 * 
 * @author Leo Przybylski
 */
public class CustomRealm extends Realm {

	private static final Logger log = Loggers.getLogger(CustomRealm.class);

	/*
	 * The type of the realm. This is defined as a static final variable to prevent typos
	 */
	public static final String TYPE = "custom";

	public static final String AUTH_HEADER = "authorization";
	public static final String USER_HEADER = "User";
	public static final String PW_HEADER = "Password";

	protected static final String OAUTH_SERVER = System.getenv().get("OAUTH_SERVER");
	protected static final String AUTHORIZATION_SERVER_URL = String.format("https://%s/oauth2/auth", OAUTH_SERVER);
	protected static final String REDIRECT_URL = System.getenv().get("REDIRECT_URL");
	protected static final String CLIENT_ID = System.getenv().get("REALM_CLIENT_ID");
	protected static final String CLIENT_SECRET = System.getenv().get("REALM_CLIENT_SECRET");

	protected static final GenericUrl USER_INFO_URL = AccessController.doPrivileged((PrivilegedAction<GenericUrl>) () -> {
		return new GenericUrl(System.getenv().get("USER_INFO_URL"));
	});
	protected static final GenericUrl TOKEN_SERVER_URL = AccessController
			.doPrivileged((PrivilegedAction<GenericUrl>) () -> {
				return new GenericUrl(String.format("https://%s/oauth2/token", OAUTH_SERVER));
			});

	/**
	 * Constructor for the Realm. This constructor delegates to the super class to initialize the common aspects such
	 * as the logger.
	 * @param config the configuration specific to this realm
	 */
	public CustomRealm(RealmConfig config) {
		this(TYPE, config);
	}

	/**
	 * This constructor should be used by extending classes so that they can specify their own specific type
	 * @param type the type of the realm
	 * @param config the configuration specific to this realm
	 */
	protected CustomRealm(String type, RealmConfig config) {
		super(type, config);
		log.warn("Kibana Custom Realm Loaded with version 0.1.38");
	}

	/**
	 * Indicates whether this realm supports the given token. This realm only support {@link UsernamePasswordToken} objects
	 * for authentication
	 * @param token the token to test for support
	 * @return true if the token is supported. false otherwise
	 */
	@Override
	public boolean supports(AuthenticationToken token) {
		log.debug("Checking to see if " + token + " is supported");
		return token instanceof UsernamePasswordToken;
	}

	/**
	 * This method will extract a token from the given {@link RestRequest} if possible. This implementation of token
	 * extraction looks for two headers, the <code>User</code> header for the username and the <code>Password</code>
	 * header for the plaintext password
	 * @param threadContext the {@link ThreadContext} that contains headers and transient objects for a request
	 * @return the {@link AuthenticationToken} if possible to extract or <code>null</code>
	 */
	@Override
	public UsernamePasswordToken token(final ThreadContext threadContext) {
		final String authStr = threadContext.getHeader(AUTH_HEADER);

		for (final Map.Entry<String, String> entry : threadContext.getHeaders().entrySet()) {
			log.debug(String.format("Header { '%s': '%s' }", entry.getKey(), entry.getValue()));
		}

		if (authStr == null) {
			log.debug("Authorization again: " + threadContext.getHeader("Authorization"));
			final UsernamePasswordToken retval = usernamePasswordToken(threadContext);
			log.debug("Using token: " + retval);
			return retval;
		}

		if (authStr.lastIndexOf(" ") < 0) {
			throw new RuntimeException("Unable to verify token from header: " + authStr);
		}

		final String authB64 = authStr.substring(authStr.lastIndexOf(" "), 1);
		final String[] authArr = new String(Base64.getDecoder().decode(authB64)).split(":");
		final String user = authArr[0];
		final String token = authArr[1];

		return new UsernamePasswordToken(user, new SecureString(token.toCharArray()));
	}

	/**
	 * Retrieves token using username/password header fields when Authorization Header does not exist
	 * 
	 * @param context {@link ThreadContext} to use to retrieve headers from 
	 */
	protected UsernamePasswordToken usernamePasswordToken(final ThreadContext context) {
		if (context.getHeader(USER_HEADER) == null && context.getHeader(PW_HEADER) == null) {
			return null;
		}

		final String username = context.getHeader(USER_HEADER);
		final String passwordStr = context.getHeader(PW_HEADER);
		final SecureString password = new SecureString(retrievePasswordFromToken(passwordStr));

		return new UsernamePasswordToken(username, password);
	}

	protected char[] retrievePasswordFromToken(final String token) {
		if (token.indexOf(":") < 0) {
			return token.toCharArray();
		}
		return token.substring(token.indexOf(":") + 1).toCharArray();
	}

	@Deprecated
	@Override
	public User authenticate(AuthenticationToken authenticationToken) {
		throw new UnsupportedOperationException("Deprecated");
	}

	/**
	 * Method that handles the actual authentication of the token. This method will only be called if the token is a
	 * supported token. The method validates the credentials of the user and if they match, a {@link User} will be
	 * returned
	 * 
	 * {@link User} if authentication is successful, otherwise <code>null</code>
	 * @param authenticationToken the token to authenticate
	 */
	@Override
	public void authenticate(AuthenticationToken authenticationToken, ActionListener<User> listener) {
		final UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
		try {
			listener.onResponse(new User(token.principal(), getGroupsFor(token.credentials())));
		} 
		catch (Exception e) {
			listener.onFailure(e);
		}
	}

	/** 
	 * Function for deriving groups through your oauth service
	 */
	protected String[] getGroupsFor(final SecureString accessToken) throws Exception {

		return new String[] {};
	}

	protected UserInfoResponse requestUserInfo(final String accessToken) throws Exception {
		final Credential creds = AccessController.doPrivileged((PrivilegedAction<Credential>) () -> {
			try {
				return new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		return AccessController.doPrivileged((PrivilegedAction<UserInfoResponse>) () -> {
			try {
				return new UserInfoRequest(new NetHttpTransport(), new JacksonFactory(), USER_INFO_URL)
						.setClientAuthentication(creds).execute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * This method looks for a user that is identified by the given String. No authentication is performed by this method.
	 * If this realm does not support user lookup, then this method will not be called.
	 * @param username the identifier for the user
	 * @return {@link User} if found, otherwise <code>null</code>
	 */
	@Override
	public User lookupUser(String username) {
		throw new RuntimeException("User Lookup not supported");
	}

	/**
	 * This method indicates whether this realm supports user lookup or not. User lookup is used for the run as functionality
	 * found in X-Pack.
	 * @return true if lookup is supported, false otherwise
	 */
	@Override
	public boolean userLookupSupported() {
		return false;
	}
}
