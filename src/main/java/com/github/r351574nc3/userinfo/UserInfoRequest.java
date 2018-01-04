package com.github.r351574nc3.realm.userinfo;

import org.apache.logging.log4j.Logger;

import org.elasticsearch.common.logging.Loggers;

import com.google.api.client.auth.oauth2.ClientParametersAuthentication;

import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.GenericData;
import com.google.api.client.util.Joiner;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;

import java.io.IOException;
import java.util.Collection;

/**
 * Request auth server to gather 
 * 
 * @author Leo Przybylski (r351574nc3 at gmail.com)
 */
public class UserInfoRequest extends GenericData {

  private static final Logger log = Loggers.getLogger(UserInfoRequest.class);

  /** HTTP request initializer or {@code null} for none. */
  HttpRequestInitializer requestInitializer;

  /** Client authentication or {@code null} for none. */
  HttpExecuteInterceptor clientAuthentication;

  /** HTTP transport. */
  private final HttpTransport transport;

  /** JSON factory. */
  private final JsonFactory jsonFactory;

  /** Token server URL. */
  private GenericUrl serverUrl;

  /**
   * Space-separated list of scopes (as specified in <a
   * href="http://tools.ietf.org/html/rfc6749#section-3.3">Access Token Scope</a>) or {@code null}
   * for none.
   */
  @Key("scope")
  private String scopes;

  /**
   * @param transport HTTP transport
   * @param jsonFactory JSON factory
   * @param serverUrl UserInfo server URL
   */
  public UserInfoRequest(HttpTransport transport, JsonFactory jsonFactory, GenericUrl serverUrl) {
    this.transport = Preconditions.checkNotNull(transport);
    this.jsonFactory = Preconditions.checkNotNull(jsonFactory);
    setServerUrl(serverUrl);
  }

  /** Returns the HTTP transport. */
  public final HttpTransport getTransport() {
    return transport;
  }

  /** Returns the JSON factory. */
  public final JsonFactory getJsonFactory() {
    return jsonFactory;
  }

  /** Returns the HTTP request initializer or {@code null} for none. */
  public final HttpRequestInitializer getRequestInitializer() {
    return requestInitializer;
  }

  /**
   * Sets the HTTP request initializer or {@code null} for none.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public UserInfoRequest setRequestInitializer(HttpRequestInitializer requestInitializer) {
    this.requestInitializer = requestInitializer;
    return this;
  }

  /** Returns the client authentication or {@code null} for none. */
  public final HttpExecuteInterceptor getClientAuthentication() {
    return clientAuthentication;
  }

  /**
   * Sets the client authentication or {@code null} for none.
   *
   * <p>
   * The recommended initializer by the specification is {@link BasicAuthentication}. All
   * authorization servers must support that. A common alternative is
   * {@link ClientParametersAuthentication}. An alternative client authentication method may be
   * provided that implements {@link HttpRequestInitializer}.
   * </p>
   *
   * <p>
   * This HTTP request execute interceptor is guaranteed to be the last execute interceptor before
   * the request is executed, and after any execute interceptor set by the
   * {@link #getRequestInitializer()}.
   * </p>
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public UserInfoRequest setClientAuthentication(HttpExecuteInterceptor clientAuthentication) {
    this.clientAuthentication = clientAuthentication;
    return this;
  }

  /** Returns the token server URL. */
  public final GenericUrl getServerUrl() {
    return serverUrl;
  }

  /**
   * Sets the UserInfo server URL.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public UserInfoRequest setServerUrl(GenericUrl serverUrl) {
    this.serverUrl = serverUrl;
    Preconditions.checkArgument(serverUrl.getFragment() == null);
    return this;
  }

  /**
   * Returns the space-separated list of scopes (as specified in <a
   * href="http://tools.ietf.org/html/rfc6749#section-3.3">Access Token Scope</a>) or {@code null}
   * for none.
   */
  public final String getScopes() {
    return scopes;
  }

  /**
   * Sets the list of scopes (as specified in <a
   * href="http://tools.ietf.org/html/rfc6749#section-3.3">Access Token Scope</a>) or {@code null}
   * for none.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   *
   * @param scopes collection of scopes to be joined by a space separator (or a single value
   *        containing multiple space-separated scopes)
   * @since 1.15
   */
  public UserInfoRequest setScopes(Collection<String> scopes) {
    this.scopes = scopes == null ? null : Joiner.on(' ').join(scopes);
    return this;
  }

  /**
   * Executes request for a UserInfo, and returns the HTTP response.
   *
   * <p>
   * To execute and parse the response to {@link UserInfoResponse}, instead use {@link #execute()}.
   * </p>
   *
   * <p>
   * Callers should call {@link HttpResponse#disconnect} when the returned HTTP response object is
   * no longer needed. However, {@link HttpResponse#disconnect} does not have to be called if the
   * response stream is properly closed. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = UserInfoRequest.executeUnparsed();
     try {
       // process the HTTP response object
     } finally {
       response.disconnect();
     }
   * </pre>
   *
   * @return successful access token response, which can then be parsed directly using
   *         {@link HttpResponse#parseAs(Class)} or some other parsing method
   * @throws UserInfoResponseException for an error response
   */
  public final HttpResponse executeUnparsed() throws IOException {
    // must set clientAuthentication as last execute interceptor in case it needs to sign request
    HttpRequestFactory requestFactory = transport.createRequestFactory(new HttpRequestInitializer() {

      public void initialize(HttpRequest request) throws IOException {
        if (requestInitializer != null) {
          requestInitializer.initialize(request);
        }
        final HttpExecuteInterceptor interceptor = request.getInterceptor();
        request.setInterceptor(new HttpExecuteInterceptor() {
          public void intercept(HttpRequest request) throws IOException {
            // Call existing interceptor
            if (interceptor != null) {
              interceptor.intercept(request);
            }
            log.warn("Client authentication: " + clientAuthentication);
            if (clientAuthentication != null) {
              clientAuthentication.intercept(request);
            }
          }
        });
      }
    });
    // make request
    HttpRequest request = requestFactory.buildPostRequest(serverUrl, new UrlEncodedContent(this));
    request.setParser(new JsonObjectParser(jsonFactory));
    request.setThrowExceptionOnExecuteError(true);
    log.warn("AUTHORIZATION: " + request.getHeaders().getAuthorization());
    HttpResponse response = request.execute();
    if (response.isSuccessStatusCode()) {
      return response;
    }
    log.warn("STATUS CODE: " + response.getStatusCode());
    log.warn("STATUS MESSAGE: " + response.getStatusMessage());
    throw UserInfoResponseException.from(jsonFactory, response);
  }

  /**
   * Executes request for an access token, and returns the parsed access token response.
   *
   * <p>
   * To execute but parse the response in an alternate way, use {@link #executeUnparsed()}.
   * </p>
   *
   * <p>
   * Default implementation calls {@link #executeUnparsed()} and then parses using
   * {@link UserInfoResponse}. Subclasses may override to change the return type, but must still call
   * {@link #executeUnparsed()}.
   * </p>
   *
   * @return parsed successful access token response
   * @throws UserInfoResponseException for an error response
   */
  public UserInfoResponse execute() throws IOException {
    return executeUnparsed().parseAs(UserInfoResponse.class);
  }

  @Override
  public UserInfoRequest set(String fieldName, Object value) {
    return (UserInfoRequest) super.set(fieldName, value);
  }
}