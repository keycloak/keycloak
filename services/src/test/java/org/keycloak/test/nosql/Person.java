package org.keycloak.test.nosql;

import java.util.List;

import org.keycloak.services.models.nosql.api.AbstractNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLCollection;
import org.keycloak.services.models.nosql.api.NoSQLField;
import org.keycloak.services.models.nosql.api.NoSQLId;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NoSQLCollection(collectionName = "persons")
public class Person extends AbstractNoSQLObject {

    private String id;
    private String firstName;
    private int age;
    private List<String> kids;
    private List<Address> addresses;

    @NoSQLId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NoSQLField
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @NoSQLField
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @NoSQLField
    public List<String> getKids() {
        return kids;
    }

    public void setKids(List<String> kids) {
        this.kids = kids;
    }

    @NoSQLField
    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }
}
