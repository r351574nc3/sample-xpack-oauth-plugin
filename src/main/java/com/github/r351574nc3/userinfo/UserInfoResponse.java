package com.github.r351574nc3.realm.userinfo;

import java.util.List;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;

/**
 * <p>
 * Implementation is not thread-safe.
 * </p>
 *
 */
public class UserInfoResponse extends GenericJson {

  @Key("email")
  protected String email;

  @Key("groups")
  protected String groups;

  @Key("name")
  protected String name;

  @Key("sub")
  protected String sub;

  @Key("tenant")
  protected Integer tenant; 

  @Key("username")
  protected String username;

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getEmail() {
    return this.email;
  }

  public void setGroups(final String groups) {
    this.groups = groups;
  }

  public String getGroups() {
    return this.groups;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public void setSub(final String sub) {
    this.sub = sub;
  }

  public String getSub() {
    return this.sub;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getUsername() {
    return this.username;
  }

  public void setTenant(final Integer tenant) {
    this.tenant = tenant;
  }

  public Integer setTenant() {
    return this.tenant;
  }
  
  @Override
  public UserInfoResponse clone() {
    return (UserInfoResponse) super.clone();
  }
}