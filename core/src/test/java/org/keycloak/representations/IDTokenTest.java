package org.keycloak.representations;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pascal Knueppel
 * @since 28.11.2025
 */
public class IDTokenTest {

    /**
     * makes sure that the setAddress and getAddress method for the idToken works as expected
     */
    @Test
    public void testSetAddressMethodWorks() {
        AddressClaimSet addressClaimSet = new AddressClaimSet();
        addressClaimSet.setFormattedAddress("test");
        addressClaimSet.setCountry("test1");
        addressClaimSet.setLocality("test2");
        addressClaimSet.setStreetAddress("test3");
        addressClaimSet.setRegion("test4");
        addressClaimSet.setPostalCode("test5");

        IDToken idToken = new IDToken();
        idToken.setAddress(addressClaimSet);

        AddressClaimSet parsedAddress = idToken.getAddress();
        Assert.assertEquals(addressClaimSet.getFormattedAddress(), parsedAddress.getFormattedAddress());
        Assert.assertEquals(addressClaimSet.getStreetAddress(), parsedAddress.getStreetAddress());
        Assert.assertEquals(addressClaimSet.getCountry(), parsedAddress.getCountry());
        Assert.assertEquals(addressClaimSet.getLocality(), parsedAddress.getLocality());
        Assert.assertEquals(addressClaimSet.getRegion(), parsedAddress.getRegion());
        Assert.assertEquals(addressClaimSet.getPostalCode(), parsedAddress.getPostalCode());

        Map<String, Object> addressClaimsSet = idToken.getAddressClaimsMap();
        Assert.assertEquals(addressClaimSet.getFormattedAddress(), addressClaimsSet.get("formatted"));
        Assert.assertEquals(addressClaimSet.getStreetAddress(), addressClaimsSet.get("street_address"));
        Assert.assertEquals(addressClaimSet.getCountry(), addressClaimsSet.get("country"));
        Assert.assertEquals(addressClaimSet.getLocality(), addressClaimsSet.get("locality"));
        Assert.assertEquals(addressClaimSet.getRegion(), addressClaimsSet.get("region"));
        Assert.assertEquals(addressClaimSet.getPostalCode(), addressClaimsSet.get("postal_code"));
    }
}
