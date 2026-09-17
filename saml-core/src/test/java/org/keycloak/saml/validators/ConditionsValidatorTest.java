package org.keycloak.saml.validators;

import java.math.BigInteger;

import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.OneTimeUseType;
import org.keycloak.dom.saml.v2.assertion.ProxyRestrictionType;

import org.junit.Assert;
import org.junit.Test;

public class ConditionsValidatorTest {

    private static final String ASSERTION_ID = "ID_assertion";

    private ConditionsValidator.Builder builder(ConditionsType conditions) {
        return new ConditionsValidator.Builder(ASSERTION_ID, conditions, DestinationValidator.forProtocolMap(null));
    }

    @Test
    public void testEmptyConditionsValid() {
        Assert.assertTrue(builder(new ConditionsType()).build().isValid());
    }

    @Test
    public void testSingleOneTimeUseValid() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(new OneTimeUseType());
        Assert.assertTrue(builder(conditions).build().isValid());
    }

    @Test
    public void testMultipleOneTimeUseRejected() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(new OneTimeUseType());
        conditions.addCondition(new OneTimeUseType());
        Assert.assertFalse(builder(conditions).build().isValid());
    }

    @Test
    public void testMultipleProxyRestrictionRejected() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(new ProxyRestrictionType());
        conditions.addCondition(new ProxyRestrictionType());
        Assert.assertFalse("More than one <ProxyRestriction> condition must make the assertion invalid",
                builder(conditions).build().isValid());
    }

    @Test
    public void testProxyRestrictionRejectedWhenValidationEnabled() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(new ProxyRestrictionType());
        Assert.assertFalse("A <ProxyRestriction> condition must be rejected by a proxying party when validation is enabled",
                builder(conditions).rejectProxyRestriction(true).build().isValid());
    }

    @Test
    public void testProxyRestrictionWithCountRejectedWhenValidationEnabled() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(proxyRestriction(BigInteger.TEN));
        Assert.assertFalse("A <ProxyRestriction> condition must be rejected regardless of its Count when validation is enabled",
                builder(conditions).rejectProxyRestriction(true).build().isValid());
    }

    @Test
    public void testProxyRestrictionAllowedWhenValidationDisabled() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(proxyRestriction(BigInteger.ZERO));
        Assert.assertTrue("A <ProxyRestriction> condition must be accepted when validation is disabled",
                builder(conditions).rejectProxyRestriction(false).build().isValid());
    }

    @Test
    public void testProxyRestrictionAllowedByDefault() {
        ConditionsType conditions = new ConditionsType();
        conditions.addCondition(new ProxyRestrictionType());
        Assert.assertTrue("A <ProxyRestriction> condition must be accepted when validation is not requested",
                builder(conditions).build().isValid());
    }

    private ProxyRestrictionType proxyRestriction(BigInteger count) {
        ProxyRestrictionType proxyRestriction = new ProxyRestrictionType();
        proxyRestriction.setCount(count);
        return proxyRestriction;
    }
}
