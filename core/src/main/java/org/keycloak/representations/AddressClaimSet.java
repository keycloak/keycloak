package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonProperty;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AddressClaimSet {
    public static final String FORMATTED = "formatted";
    public static final String STREET_ADDRESS = "street_address";
    public static final String LOCALITY = "locality";
    public static final String REGION = "region";
    public static final String POSTAL_CODE = "postal_code";
    public static final String COUNTRY = "country";

    @JsonProperty(FORMATTED)
    protected String formattedAddress;

    @JsonProperty(STREET_ADDRESS)
    protected String streetAddress;

    @JsonProperty(LOCALITY)
    protected String locality;

    @JsonProperty(REGION)
    protected String region;

    @JsonProperty(POSTAL_CODE)
    protected String postalCode;

    @JsonProperty(COUNTRY)
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
