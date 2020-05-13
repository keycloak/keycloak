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
public class LDAPOid {

  private final Object oid;

  public LDAPOid(Object oidValue) {
    this.oid = Objects.requireNonNull(oidValue);
  }

  public String getOid() {
    return oid instanceof String ? (String) oid : String.valueOf(oid);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LDAPOid ldapOid = (LDAPOid) o;
    return oid.equals(ldapOid.oid);
  }

  @Override
  public int hashCode() {
    return oid.hashCode();
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", LDAPOid.class.getSimpleName() + "[", "]")
        .add("oid=" + oid)
        .toString();
  }
}
