/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.test.broker.oidc.mappers;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.mappers.UsernameTemplateMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper.Target;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.keycloak.broker.saml.mappers.UsernameTemplateMapper.TARGET;

/**
 * Unit test for {@link UsernameTemplateMapper} making sure a username template that cannot be resolved never
 * results in a blank username.
 */
public class UsernameTemplateMapperTest {

    private static final String ALIAS = "myidp";

    private final UsernameTemplateMapper mapper = new UsernameTemplateMapper();

    @Test
    public void resolvedTemplateSetsUsername() {
        BrokeredIdentityContext context = context("preferred_username", "jdoe");

        mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${ALIAS}-${CLAIM.preferred_username}"), context);

        assertEquals(ALIAS + "-jdoe", context.getModelUsername());
    }

    @Test
    public void missingClaimIsRejected() {
        BrokeredIdentityContext context = context("preferred_username", "jdoe");
        context.setModelUsername("previous-username");

        IdentityBrokerException e = assertThrows(IdentityBrokerException.class,
                () -> mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${CLAIM.non-existent-claim}"), context));

        assertTrue(e.getMessage(), e.getMessage().contains("${CLAIM.non-existent-claim}"));
        assertEquals("the existing username must not be overwritten", "previous-username", context.getModelUsername());
    }

    @Test
    public void partiallyMissingClaimIsRejected() {
        BrokeredIdentityContext context = context("preferred_username", "jdoe");

        assertThrows(IdentityBrokerException.class,
                () -> mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${ALIAS}-${CLAIM.non-existent-claim}"), context));

        assertNull("an incomplete username must not be applied", context.getModelUsername());
    }

    @Test
    public void emptyClaimValueIsRejected() {
        BrokeredIdentityContext context = context("preferred_username", "");

        assertThrows(IdentityBrokerException.class,
                () -> mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${CLAIM.preferred_username}"), context));

        assertNull(context.getModelUsername());
    }

    @Test
    public void brokerTargetsAreApplied() {
        BrokeredIdentityContext id = context("preferred_username", "jdoe");
        mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.BROKER_ID, "${CLAIM.preferred_username}"), id);
        assertEquals("jdoe", id.getId());

        BrokeredIdentityContext name = context("preferred_username", "jdoe");
        mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.BROKER_USERNAME, "${CLAIM.preferred_username}"), name);
        assertEquals("jdoe", name.getUsername());
    }

    @Test
    public void updateBrokeredUserUpdatesUsername() {
        BrokeredIdentityContext context = context("preferred_username", "jdoe");
        context.setModelUsername(ALIAS + "-jdoe");
        List<String> written = new ArrayList<>();

        mapper.updateBrokeredUser(null, realm(false), user(written), mapperModel(Target.LOCAL, "${ALIAS}"), context);

        assertEquals(List.of(ALIAS + "-jdoe"), written);
    }

    @Test
    public void updateBrokeredUserNeverBlanksExistingUsername() {
        for (String username : new String[]{null, "", "  "}) {
            BrokeredIdentityContext context = context("preferred_username", "jdoe");
            context.setModelUsername(username);
            List<String> written = new ArrayList<>();

            mapper.updateBrokeredUser(null, realm(false), user(written), mapperModel(Target.LOCAL, "${ALIAS}"), context);

            assertEquals("username [" + username + "] must not be written back", List.of(), written);
        }
    }

    @Test
    public void updateBrokeredUserKeepsUsernameWhenEmailIsUsedAsUsername() {
        BrokeredIdentityContext context = context("preferred_username", "jdoe");
        context.setModelUsername(ALIAS + "-jdoe");
        List<String> written = new ArrayList<>();

        mapper.updateBrokeredUser(null, realm(true), user(written), mapperModel(Target.LOCAL, "${ALIAS}"), context);

        assertEquals(List.of(), written);
    }

    private BrokeredIdentityContext context(String claimName, Object claimValue) {
        IdentityProviderModel idpConfig = new IdentityProviderModel();
        idpConfig.setAlias(ALIAS);
        idpConfig.setEnabled(true);

        JsonWebToken idToken = new JsonWebToken();
        idToken.setSubject("sub-1");
        idToken.getOtherClaims().put(claimName, claimValue);

        BrokeredIdentityContext context = new BrokeredIdentityContext("sub-1", idpConfig);
        context.getContextData().put(OIDCIdentityProvider.VALIDATED_ID_TOKEN, idToken);
        return context;
    }

    private IdentityProviderMapperModel mapperModel(Target target, String template) {
        Map<String, String> config = new HashMap<>();
        config.put(UsernameTemplateMapper.TEMPLATE, template);
        config.put(TARGET, target.name());
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        model.setConfig(config);
        return model;
    }

    private RealmModel realm(boolean registrationEmailAsUsername) {
        return stub(RealmModel.class, (method, args) ->
                "isRegistrationEmailAsUsername".equals(method.getName()) ? (Object) registrationEmailAsUsername : null);
    }

    private UserModel user(List<String> writtenUsernames) {
        return stub(UserModel.class, (method, args) -> {
            if ("setUsername".equals(method.getName())) {
                writtenUsernames.add((String) args[0]);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> iface, BiFunction<Method, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface},
                (proxy, method, args) -> handler.apply(method, args));
    }

}