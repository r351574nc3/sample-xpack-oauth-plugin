package com.github.r351574nc3.realm.userinfo;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;

/**
 * <p>
 * Implementation is not thread-safe.
 * </p>
 *
 */
public class UserInfoErrorResponse extends GenericJson {
  
  /**
   * Error code ({@code "invalid_request"}, {@code "invalid_client"}, {@code "invalid_grant"},
   * {@code "unauthorized_client"}, {@code "unsupported_grant_type"}, {@code "invalid_scope"}, or an
   * extension error code as specified in <a
   * href="http://tools.ietf.org/html/rfc6749#section-8.5">Defining Additional Error Codes</a>).
   */
  @Key
  private String error;

  /**
   * Human-readable text providing additional information, used to assist the client developer in
   * understanding the error that occurred or {@code null} for none.
   */
  @Key("error_description")
  private String errorDescription;

  /**
   * URI identifying a human-readable web page with information about the error, used to provide the
   * client developer with additional information about the error or {@code null} for none.
   */
  @Key("error_uri")
  private String errorUri;

  /**
   * Returns the error code ({@code "invalid_request"}, {@code "invalid_client"},
   * {@code "invalid_grant"}, {@code "unauthorized_client"}, {@code "unsupported_grant_type"},
   * {@code "invalid_scope"}, or an extension error code as specified in <a
   * href="http://tools.ietf.org/html/rfc6749#section-8.5">Defining Additional Error Codes</a>).
   */
  public final String getError() {
    return error;
  }

  /**
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public UserInfoErrorResponse setError(String error) {
    this.error = Preconditions.checkNotNull(error);
    return this;
  }

  /**
   * Returns the human-readable text providing additional information, used to assist the client
   * developer in understanding the error that occurred or {@code null} for none.
   */
  public final String getErrorDescription() {
    return errorDescription;
  }

  /**
   * Sets the human-readable text providing additional information, used to assist the client
   * developer in understanding the error that occurred or {@code null} for none.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public UserInfoErrorResponse setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
    return this;
  }

  /**
   * Returns the URI identifying a human-readable web page with information about the error, used to
   * provide the client developer with additional information about the error or {@code null} for
   * none.
   */
  public final String getErrorUri() {
    return errorUri;
  }

  /**
   * Sets the URI identifying a human-readable web page with information about the error, used to
   * provide the client developer with additional information about the error or {@code null} for
   * none.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public UserInfoErrorResponse setErrorUri(String errorUri) {
    this.errorUri = errorUri;
    return this;
  }

  @Override
  public UserInfoErrorResponse set(String fieldName, Object value) {
    return (UserInfoErrorResponse) super.set(fieldName, value);
  }

  @Override
  public UserInfoErrorResponse clone() {
    return (UserInfoErrorResponse) super.clone();
  }
}