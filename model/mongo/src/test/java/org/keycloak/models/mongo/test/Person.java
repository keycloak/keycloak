package org.keycloak.models.mongo.test;

import java.util.List;

import org.keycloak.models.mongo.api.AbstractNoSQLObject;
import org.keycloak.models.mongo.api.NoSQLCollection;
import org.keycloak.models.mongo.api.NoSQLField;
import org.keycloak.models.mongo.api.NoSQLId;

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
    private Address mainAddress;
    private Gender gender;
    private List<Gender> genders;


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
    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @NoSQLField
    public List<Gender> getGenders() {
        return genders;
    }

    public void setGenders(List<Gender> genders) {
        this.genders = genders;
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

    @NoSQLField
    public Address getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(Address mainAddress) {
        this.mainAddress = mainAddress;
    }

    public static enum Gender {
        MALE, FEMALE
    }
}
