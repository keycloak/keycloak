package org.keycloak.tests.admin.identityprovider;

import java.util.Map;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;

public class AbstractIdentityProviderTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    protected String create(IdentityProviderRepresentation idpRep) {
        String idpId = ApiUtil.getCreatedId(managedRealm.admin().identityProviders().create(idpRep));
        Assertions.assertNotNull(idpId);

        String secret = idpRep.getConfig() != null ? idpRep.getConfig().get("clientSecret") : null;
        idpRep = StripSecretsUtils.stripSecrets(null, idpRep);

        if ("true".equals(idpRep.getConfig().get(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR))) {
            idpRep.setHideOnLogin(true);
            idpRep.getConfig().remove(IdentityProviderModel.LEGACY_HIDE_ON_LOGIN_ATTR);
        }

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.identityProviderPath(idpRep.getAlias()), idpRep, ResourceType.IDENTITY_PROVIDER);

        if (secret != null) {
            idpRep.getConfig().put("clientSecret", secret);
        }

        return idpId;
    }

    protected IdentityProviderRepresentation createRep(String alias, String providerId) {
        return createRep(alias, providerId,true, null);
    }

    protected IdentityProviderRepresentation createRep(String alias, String providerId,boolean enabled, Map<String, String> config) {
        return createRep(alias, alias, providerId, enabled, config);
    }

    protected IdentityProviderRepresentation createRep(String alias, String displayName, String providerId, boolean enabled, Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();

        idp.setAlias(alias);
        idp.setDisplayName(displayName);
        idp.setProviderId(providerId);
        idp.setEnabled(enabled);
        if (config != null) {
            idp.setConfig(config);
        }
        return idp;
    }

    protected void assertProviderInfo(Map<String, String> info, String id, String name) {
        System.out.println(info);
        Assertions.assertEquals(id, info.get("id"), "id");
        Assertions.assertEquals(name, info.get("name"), "name");
    }
}
