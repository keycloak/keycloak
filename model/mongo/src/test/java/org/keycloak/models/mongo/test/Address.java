package org.keycloak.models.mongo.test;

import org.keycloak.models.mongo.api.AbstractNoSQLObject;
import org.keycloak.models.mongo.api.NoSQLField;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Address extends AbstractNoSQLObject {

    private String street;
    private int number;
    private List<String> flatNumbers;

    @NoSQLField
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @NoSQLField
    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @NoSQLField
    public List<String> getFlatNumbers() {
        return flatNumbers;
    }

    public void setFlatNumbers(List<String> flatNumbers) {
        this.flatNumbers = flatNumbers;
    }
}
