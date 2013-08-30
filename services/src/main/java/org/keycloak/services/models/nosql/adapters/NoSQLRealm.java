package org.keycloak.services.models.nosql.adapters;

import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;
import org.keycloak.services.models.nosql.api.NoSQLObject;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "realms")
public class NoSQLRealm implements NoSQLObject {

    private String oid;
    private String prop1;
    private Integer prop2;

    @NoSQLId
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @NoSQLField(fieldName = "property1")
    public String getProp1() {
        return prop1;
    }

    public void setProp1(String prop1) {
        this.prop1 = prop1;
    }

    @NoSQLField(fieldName = "property2")
    public Integer getProp2() {
        return prop2;
    }

    public void setProp2(Integer prop2) {
        this.prop2 = prop2;
    }

    @Override
    public String toString() {
        return "NoSQLRealm [ oid=" + oid + ", prop1=" + prop1 + ", prop2=" + prop2 + "]";
    }
}
