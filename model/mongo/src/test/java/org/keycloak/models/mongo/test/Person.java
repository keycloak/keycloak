package org.keycloak.models.mongo.test;

import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "persons")
public class Person extends AbstractMongoIdentifiableEntity {

    private String firstName;
    private int age;
    private List<String> kids;
    private List<Address> addresses;
    private Address mainAddress;
    private Gender gender;
    private List<Gender> genders;
    private Map<String, String> attributes = new HashMap<String, String>();

    @MongoField
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @MongoField
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @MongoField
    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @MongoField
    public List<Gender> getGenders() {
        return genders;
    }

    public void setGenders(List<Gender> genders) {
        this.genders = genders;
    }

    @MongoField
    public List<String> getKids() {
        return kids;
    }

    public void setKids(List<String> kids) {
        this.kids = kids;
    }

    @MongoField
    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    @MongoField
    public Address getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(Address mainAddress) {
        this.mainAddress = mainAddress;
    }

    @MongoField
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public static enum Gender {
        MALE, FEMALE
    }
}
