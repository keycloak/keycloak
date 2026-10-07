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
}
