package org.keycloak.broker.oidc;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.social.microsoft.MicrosoftIdentityProviderConfig;
import org.keycloak.social.paypal.PayPalIdentityProviderConfig;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OAuth2IdentityProviderConfigMaskedSecretTest {

    @Test
    public void rejectsWhenTokenUrlChanges() {
        OAuth2IdentityProviderConfig updated = new OIDCIdentityProviderConfig();
        updated.getConfig().put("tokenUrl", "https://attacker.example/token");
        updated.getConfig().put("clientId", "clientId");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        updated.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("tokenUrl", "https://idp.example.com/token");
        stored.getConfig().put("clientId", "clientId");
        stored.getConfig().put("clientSecret", "real-partner-secret");
        stored.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);

        assertFalse(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void allowsWhenUnchanged() {
        OAuth2IdentityProviderConfig updated = new OIDCIdentityProviderConfig();
        updated.getConfig().put("tokenUrl", "https://idp.example.com/token");
        updated.getConfig().put("clientId", "clientId");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        updated.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("tokenUrl", "https://idp.example.com/token");
        stored.getConfig().put("clientId", "clientId");
        stored.getConfig().put("clientSecret", "real-partner-secret");
        stored.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);

        assertTrue(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void rejectsWhenMicrosoftTenantIdChanges() {
        MicrosoftIdentityProviderConfig updated = new MicrosoftIdentityProviderConfig();
        updated.getConfig().put("tenantId", "attacker-tenant");
        updated.getConfig().put("clientId", "microsoft-client");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("tenantId", "common");
        stored.getConfig().put("clientId", "microsoft-client");
        stored.getConfig().put("clientSecret", "real-microsoft-secret");

        assertFalse(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void rejectsWhenPayPalSandboxChanges() {
        PayPalIdentityProviderConfig updated = new PayPalIdentityProviderConfig();
        updated.getConfig().put("sandbox", "true");
        updated.getConfig().put("clientId", "paypal-client");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("sandbox", "false");
        stored.getConfig().put("clientId", "paypal-client");
        stored.getConfig().put("clientSecret", "real-paypal-secret");

        assertFalse(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void plainIdentityProviderModelAlwaysAllows() {
        IdentityProviderModel updated = new IdentityProviderModel();
        updated.getConfig().put("tokenUrl", "https://attacker.example/token");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("tokenUrl", "https://idp.example.com/token");
        stored.getConfig().put("clientSecret", "real-partner-secret");

        assertTrue(updated.canReuseMaskedClientSecret(stored));
        assertTrue(stored.canReuseMaskedClientSecret(updated));
    }

    @Test
    public void allowsWhenPayPalSandboxAbsentAndSubmittedAsFalse() {
        PayPalIdentityProviderConfig updated = new PayPalIdentityProviderConfig();
        updated.getConfig().put("sandbox", "false");
        updated.getConfig().put("clientId", "paypal-client");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("clientId", "paypal-client");
        stored.getConfig().put("clientSecret", "real-paypal-secret");

        assertTrue(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void rejectsWhenPayPalSandboxChangesFromUnparseableToTrue() {
        // runtime uses Boolean.valueOf on the raw value, so "true " targets production
        PayPalIdentityProviderConfig updated = new PayPalIdentityProviderConfig();
        updated.getConfig().put("sandbox", "true");
        updated.getConfig().put("clientId", "paypal-client");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("sandbox", "true ");
        stored.getConfig().put("clientId", "paypal-client");
        stored.getConfig().put("clientSecret", "real-paypal-secret");

        assertFalse(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void allowsWhenMicrosoftTenantIdAbsentAndSubmittedAsCommonOrEmpty() {
        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("clientId", "ms-client");
        stored.getConfig().put("clientSecret", "real-ms-secret");

        MicrosoftIdentityProviderConfig empty = new MicrosoftIdentityProviderConfig();
        empty.getConfig().put("tenantId", "");
        empty.getConfig().put("clientId", "ms-client");
        empty.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        assertTrue(empty.canReuseMaskedClientSecret(stored));

        // whitespace-only is not treated as absent at runtime and yields a different token path
        MicrosoftIdentityProviderConfig blank = new MicrosoftIdentityProviderConfig();
        blank.getConfig().put("tenantId", "  ");
        blank.getConfig().put("clientId", "ms-client");
        blank.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        assertFalse(blank.canReuseMaskedClientSecret(stored));

        MicrosoftIdentityProviderConfig common = new MicrosoftIdentityProviderConfig();
        common.getConfig().put("tenantId", "common");
        common.getConfig().put("clientId", "ms-client");
        common.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);
        assertTrue(common.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void rejectsWhenClientIdDiffersOnlyByWhitespace() {
        // runtime sends the raw client ID, so whitespace is a real change
        OAuth2IdentityProviderConfig updated = new OIDCIdentityProviderConfig();
        updated.getConfig().put("tokenUrl", "https://idp.example.com/token");
        updated.getConfig().put("clientId", " clientId ");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("tokenUrl", "https://idp.example.com/token");
        stored.getConfig().put("clientId", "clientId");
        stored.getConfig().put("clientSecret", "real-partner-secret");

        assertFalse(updated.canReuseMaskedClientSecret(stored));
    }

    @Test
    public void allowsWhenClientAuthMethodAbsentOrEmptyAndSubmittedAsPost() {
        IdentityProviderModel storedEmpty = new IdentityProviderModel();
        storedEmpty.getConfig().put("tokenUrl", "https://idp.example.com/token");
        storedEmpty.getConfig().put("clientId", "clientId");
        storedEmpty.getConfig().put("clientAuthMethod", "");
        storedEmpty.getConfig().put("clientSecret", "real-partner-secret");

        IdentityProviderModel storedAbsent = new IdentityProviderModel();
        storedAbsent.getConfig().put("tokenUrl", "https://idp.example.com/token");
        storedAbsent.getConfig().put("clientId", "clientId");
        storedAbsent.getConfig().put("clientSecret", "real-partner-secret");

        OAuth2IdentityProviderConfig updated = new OIDCIdentityProviderConfig();
        updated.getConfig().put("tokenUrl", "https://idp.example.com/token");
        updated.getConfig().put("clientId", "clientId");
        updated.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_POST);
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        assertTrue(updated.canReuseMaskedClientSecret(storedEmpty));
        assertTrue(updated.canReuseMaskedClientSecret(storedAbsent));

        updated.getConfig().put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_BASIC);
        assertFalse(updated.canReuseMaskedClientSecret(storedEmpty));
        assertFalse(updated.canReuseMaskedClientSecret(storedAbsent));
    }

    @Test
    public void allowsWhenOptionalDestinationAbsentAndSubmittedAsEmpty() {
        OAuth2IdentityProviderConfig updated = new OIDCIdentityProviderConfig();
        updated.getConfig().put("tokenUrl", "https://idp.example.com/token");
        updated.getConfig().put("tokenIntrospectionUrl", "");
        updated.getConfig().put("baseUrl", "");
        updated.getConfig().put("clientId", "clientId");
        updated.getConfig().put("clientSecret", ComponentRepresentation.SECRET_VALUE);

        IdentityProviderModel stored = new IdentityProviderModel();
        stored.getConfig().put("tokenUrl", "https://idp.example.com/token");
        stored.getConfig().put("clientId", "clientId");
        stored.getConfig().put("clientSecret", "real-partner-secret");

        assertTrue(updated.canReuseMaskedClientSecret(stored));
    }
}
