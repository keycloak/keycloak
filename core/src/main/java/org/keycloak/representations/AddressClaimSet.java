package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonProperty;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AddressClaimSet {
    @JsonProperty("formatted")
    protected String formattedAddress;

    @JsonProperty("street_address")
    protected String streetAddress;

    @JsonProperty("locality")
    protected String locality;

    @JsonProperty("region")
    protected String region;

    @JsonProperty("postal_code")
    protected String postalCode;

    @JsonProperty("country")
    protected String country;

    public String getFormattedAddress() {
        return this.formattedAddress;
    }

    public void setFormattedAddress(String formattedAddress) {
        this.formattedAddress = formattedAddress;
    }

    public String getStreetAddress() {
        return this.streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getLocality() {
        return this.locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getRegion() {
        return this.region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPostalCode() {
        return this.postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

}
