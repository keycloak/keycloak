package org.keycloak.models.mongo.test;

import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Address implements MongoEntity {

    private String street;
    private int number;
    private List<String> flatNumbers;

    @MongoField
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @MongoField
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @MongoField
    public List<String> getFlatNumbers() {
        return flatNumbers;
    }

    public void setFlatNumbers(List<String> flatNumbers) {
        this.flatNumbers = flatNumbers;
    }
}
