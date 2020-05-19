package org.keycloak.storage.ldap.idm.model;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Value object to represent an OID (object identifier) as used to describe LDAP schema, extension and features.
 * See <a href="https://ldap.com/ldap-oid-reference-guide/">LDAP OID Reference Guide</a>.
 *
 * @author Lars Uffmann, 2020-05-13
 * @since 11.0
 */
public class LDAPCapability {

  public enum CapabilityType {
    CONTROL,
    EXTENSION,
    FEATURE,
    UNKNOWN;

    public static CapabilityType fromRootDseAttributeName(String attributeName) {
      switch (attributeName) {
        case "supportedExtension": return CapabilityType.EXTENSION;
        case "supportedControl": return CapabilityType.CONTROL;
        case "supportedFeatures": return CapabilityType.FEATURE;
        default: return CapabilityType.UNKNOWN;
      }
    }
  };

  private final Object oid;

  private final CapabilityType type;

  public LDAPCapability(Object oidValue, CapabilityType type) {
    this.oid = Objects.requireNonNull(oidValue);
    this.type = type;
  }

  public String getOid() {
    return oid instanceof String ? (String) oid : String.valueOf(oid);
  }

  public CapabilityType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LDAPCapability ldapOid = (LDAPCapability) o;
    return oid.equals(ldapOid.oid);
  }

  @Override
  public int hashCode() {
    return oid.hashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", LDAPCapability.class.getSimpleName() + "[", "]")
        .add("oid=" + oid)
        .add("type=" + type)
        .toString();
  }
}
