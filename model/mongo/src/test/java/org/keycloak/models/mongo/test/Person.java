package org.keycloak.models.mongo.test;

import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "persons")
public class Person implements MongoIdentifiableEntity {

    private String id;
    private String firstName;
    private int age;
    private List<String> kids;
    private List<AddressWithFlats> addresses;
    private Address mainAddress;
    private Gender gender;
    private List<Gender> genders;
    private Map<String, String> attributes = new HashMap<String, String>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public List<Gender> getGenders() {
        return genders;
    }

    public void setGenders(List<Gender> genders) {
        this.genders = genders;
    }

    public List<String> getKids() {
        return kids;
    }

    public void setKids(List<String> kids) {
        this.kids = kids;
    }

    public List<AddressWithFlats> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AddressWithFlats> addresses) {
        this.addresses = addresses;
    }

    public Address getMainAddress() {
        return mainAddress;
    }

    public void setMainAddress(Address mainAddress) {
        this.mainAddress = mainAddress;
    }

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

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
    }

    public static enum Gender {
        MALE, FEMALE
    }
}
