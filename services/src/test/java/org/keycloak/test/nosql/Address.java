package org.keycloak.test.nosql;

import java.util.List;

import org.keycloak.services.models.nosql.api.AbstractNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLField;

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
