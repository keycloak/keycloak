package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.oidc.mappers.UsernameTemplateMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import com.google.common.collect.ImmutableMap;

/**
 * @author <a href="mailto:external.martin.idel@bosch.io">Martin Idel</a>
 */
public class KcOidcUsernameTemplateMapperTest extends AbstractUsernameTemplateMapperTest {
    @Override
    protected void createMapperInIdp(IdentityProviderRepresentation idp, IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderMapperRepresentation usernameTemplateMapper = new IdentityProviderMapperRepresentation();
        usernameTemplateMapper.setName("oidc-username-template-mapper");
        usernameTemplateMapper.setIdentityProviderMapper(UsernameTemplateMapper.PROVIDER_ID);
        usernameTemplateMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, syncMode.toString())
                .put("template", "${ALIAS}-${CLAIM.user-attribute}")
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        usernameTemplateMapper.setIdentityProviderAlias(bc.getIDPAlias());
        idpResource.addMapper(usernameTemplateMapper).close();
    }

    @Override
    protected String getMapperTemplate() {
        return "kc-oidc-idp-[%s]";
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }
}
