package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

public class UserRolesBodyRepresentation {
  private String clientId;
  private String orgId;

  @JsonSetter(nulls = Nulls.SKIP)
  private List<FilterAttributeRepresentation> filterAttributes = Collections.emptyList();

  @JsonSetter(nulls = Nulls.SKIP)
  private Set<String> byRoles = new HashSet<>();

  @JsonSetter(nulls = Nulls.SKIP)
  private List<String> sortBy = new ArrayList<>(Collections.singleton("username"));

  @JsonSetter(nulls = Nulls.SKIP)
  private Integer first = 0;

  @JsonSetter(nulls = Nulls.SKIP)
  private Integer max = 10;

  public Integer getMax() {
    return max;
  }

  public void setMax(Integer max) {
    this.max = max;
  }

  public Integer getFirst() {
    return first;
  }

  public void setFirst(Integer first) {
    this.first = first;
  }

  public List<String> getSortBy() {
    return sortBy;
  }

  public void setSortBy(List<String> sortBy) {
    this.sortBy = sortBy;
  }

  public Set<String> getByRoles() {
    return byRoles;
  }

  public void setByRoles(Set<String> byRoles) {
    this.byRoles = byRoles;
  }

  public List<FilterAttributeRepresentation> getFilterAttributes() {
    return filterAttributes;
  }

  public void setFilterAttributes(List<FilterAttributeRepresentation> filterAttributes) {
    this.filterAttributes = filterAttributes;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }
}
