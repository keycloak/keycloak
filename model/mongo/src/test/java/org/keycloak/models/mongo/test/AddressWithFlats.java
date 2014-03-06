package org.keycloak.models.mongo.test;

import java.util.List;

import org.keycloak.models.mongo.api.MongoField;

/**
 * Just to test inheritance
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AddressWithFlats extends Address {

    private List<String> flatNumbers;

    @MongoField
    public List<String> getFlatNumbers() {
        return flatNumbers;
    }

    public void setFlatNumbers(List<String> flatNumbers) {
        this.flatNumbers = flatNumbers;
    }
}
