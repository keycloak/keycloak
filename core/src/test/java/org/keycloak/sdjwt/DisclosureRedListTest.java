package org.keycloak.sdjwt;

import org.junit.Test;

public class DisclosureRedListTest {

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInObjectClaim() {
        DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("vct")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInArrayClaim() {
        DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedArrayElt("iat", 0, "2GLC42sKQveCfGfryNRN9w")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInDecoyArrayClaim() {
        DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withDecoyArrayElt("exp", 0, "2GLC42sKQveCfGfryNRN9w")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedIss() {
        DisclosureSpec.builder().withUndisclosedClaim("iss").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInObjectNbf() {
        DisclosureSpec.builder().withUndisclosedClaim("nbf").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedCnf() {
        DisclosureSpec.builder().withUndisclosedClaim("cnf").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedStatus() {
        DisclosureSpec.builder().withUndisclosedClaim("status").build();
    }
}
