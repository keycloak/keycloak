package org.keycloak.representations.idm;

import java.util.List;

public class FilterAttributeRepresentation {
  private String key;
  private List<String> values;
  private Boolean exact;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getValues() {
    return values;
  }

  public void setValues(List<String> values) {
    this.values = values;
  }

  public Boolean getExact() {
    return exact;
  }

  public void setExact(Boolean exact) {
    this.exact = exact;
  }
}
