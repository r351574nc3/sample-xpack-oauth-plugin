package com.github.r351574nc3.realm.userinfo;

import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.StringUtils;

import java.io.IOException;


/**
 * <p>
 * To get the structured details, use {@link #getDetails()}.
 * </p>
 *
 */
public class UserInfoResponseException extends HttpResponseException {

  private static final long serialVersionUID = 4020689092957439244L;

  /** UserInfo error response details or {@code null} if unable to parse. */
  private final transient UserInfoErrorResponse details;

  /**
   * @param builder builder
   * @param details UserInfo error response details or {@code null} if unable to parse
   */
  UserInfoResponseException(Builder builder, UserInfoErrorResponse details) {
    super(builder);
    this.details = details;
  }

  /** Returns the UserInfo error response details or {@code null} if unable to parse. */
  public final UserInfoErrorResponse getDetails() {
    return details;
  }

  /**
   * Returns a new instance of {@link UserInfoResponseException}.
   *
   * <p>
   * If there is a JSON error response, it is parsed using {@link UserInfoErrorResponse}, which can be
   * inspected using {@link #getDetails()}. Otherwise, the full response content is read and
   * included in the exception message.
   * </p>
   *
   * @param jsonFactory JSON factory
   * @param response HTTP response
   * @return new instance of {@link UserInfoErrorResponse}
   */
  public static UserInfoResponseException from(JsonFactory jsonFactory, HttpResponse response) {
    HttpResponseException.Builder builder = new HttpResponseException.Builder(
        response.getStatusCode(), response.getStatusMessage(), response.getHeaders());
    // details
    Preconditions.checkNotNull(jsonFactory);
    UserInfoErrorResponse details = null;
    String detailString = null;
    String contentType = response.getContentType();
    try {
      if (!response.isSuccessStatusCode() && contentType != null && response.getContent() != null
          && HttpMediaType.equalsIgnoreParameters(Json.MEDIA_TYPE, contentType)) {
        details = new JsonObjectParser(jsonFactory).parseAndClose(
            response.getContent(), response.getContentCharset(), UserInfoErrorResponse.class);
        detailString = details.toPrettyString();
      } else {
        detailString = response.parseAsString();
      }
    } catch (IOException exception) {
      // it would be bad to throw an exception while throwing an exception
      exception.printStackTrace();
    }
    // message
    StringBuilder message = HttpResponseException.computeMessageBuffer(response);
    if (!com.google.api.client.util.Strings.isNullOrEmpty(detailString)) {
      message.append(StringUtils.LINE_SEPARATOR).append(detailString);
      builder.setContent(detailString);
    }
    builder.setMessage(message.toString());
    return new UserInfoResponseException(builder, details);
  }
}