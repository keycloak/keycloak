package org.keycloak.testsuite.federation.ldap;

import jakarta.ws.rs.BadRequestException;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.testsuite.util.LDAPRule;

/**
 * @author <a href="mailto:m.neuhaus@smf.de">Marco Neuhaus</a>
 */
public class LDAPCredentialsTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {

    }

    @After
    public void resetLDAPConfigAfterTest() {
        // cleanup
        ComponentRepresentation representation = getLdapConfigRep();
        ldapRule.getConfig().forEach((key, value) -> representation.getConfig().putSingle(key, value));
        updateConfigRep(representation);
    }

    @Test
    public void testUpdateAuthTypeClearsCredentials() {
        ComponentRepresentation model = getLdapConfigRep();
        LDAPConfig ldapConfig = new LDAPConfig(model.getConfig());

        Assert.assertEquals(LDAPConstants.AUTH_TYPE_SIMPLE, ldapConfig.getAuthType());
        Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, ldapConfig.getBindCredential());

        // set auth type to none
        model.getConfig().putSingle(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_NONE);
        updateConfigRep(model);

        model = getLdapConfigRep();
        ldapConfig = new LDAPConfig(model.getConfig());

        // expected the credential is removed
        Assert.assertEquals(LDAPConstants.AUTH_TYPE_NONE, ldapConfig.getAuthType());
        Assert.assertNull(ldapConfig.getBindCredential());

        // reset auth type to simple
        model.getConfig().putSingle(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_SIMPLE);
        updateConfigRep(model);

        model = getLdapConfigRep();
        ldapConfig = new LDAPConfig(model.getConfig());

        // expected the credential still null
        Assert.assertEquals(LDAPConstants.AUTH_TYPE_SIMPLE, ldapConfig.getAuthType());
        Assert.assertNull(ldapConfig.getBindCredential());
    }

    @Test
    public void testChangeConnectionUrlWithAuthTypeSimple() {
        ComponentRepresentation model = getLdapConfigRep();

        Assert.assertEquals(LDAPConstants.AUTH_TYPE_SIMPLE, model.getConfig().getFirst(LDAPConstants.AUTH_TYPE));
        Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, model.getConfig().getFirst(LDAPConstants.BIND_CREDENTIAL));

        model.getConfig().putSingle(LDAPConstants.CONNECTION_URL, "ldap://other:10389");
        expectedBadRequest(model);

        model.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, ldapRule.getConfig().get(LDAPConstants.BIND_CREDENTIAL));
        updateConfigRep(model);
    }

    @Test
    public void testChangeConnectionUrlWithoutCredential() {
        ComponentRepresentation model = getLdapConfigRep();

        Assert.assertEquals(LDAPConstants.AUTH_TYPE_SIMPLE, model.getConfig().getFirst(LDAPConstants.AUTH_TYPE));
        Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, model.getConfig().getFirst(LDAPConstants.BIND_CREDENTIAL));

        model.getConfig().putSingle(LDAPConstants.CONNECTION_URL, "ldap://other:10389");
        model.getConfig().remove(LDAPConstants.BIND_CREDENTIAL);
        expectedBadRequest(model);

        model.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, ldapRule.getConfig().get(LDAPConstants.BIND_CREDENTIAL));
        updateConfigRep(model);
    }

    @Test
    public void testChangeBindDN() {
        ComponentRepresentation model = getLdapConfigRep();
        Assert.assertEquals(ComponentRepresentation.SECRET_VALUE, model.getConfig().getFirst(LDAPConstants.BIND_CREDENTIAL));

        model.getConfig().putSingle(LDAPConstants.BIND_DN, "abc");
        expectedBadRequest(model); //expected validation error

        model.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=admin,dc=example,dc=org");
        model.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, ldapRule.getConfig().get(LDAPConstants.BIND_CREDENTIAL));
        updateConfigRep(model);
    }

    @Test
    public void testChangeConnectionUrlWithAuthTypeNone() {
        ComponentRepresentation model = getLdapConfigRep();

        model.getConfig().putSingle(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_NONE);
        updateConfigRep(model);

        model = getLdapConfigRep();

        model.getConfig().putSingle(LDAPConstants.CONNECTION_URL, "ldap://other:10389");
        updateConfigRep(model); //no validation error
    }

    private void expectedBadRequest(ComponentRepresentation representation) {
        Assert.assertThrows(BadRequestException.class, () -> updateConfigRep(representation));
    }

    private void updateConfigRep(ComponentRepresentation representation) {
        testRealm().components().component(ldapModelId).update(representation);
    }

    private ComponentRepresentation getLdapConfigRep() {
        return testRealm().components().component(ldapModelId).toRepresentation();
    }
}
