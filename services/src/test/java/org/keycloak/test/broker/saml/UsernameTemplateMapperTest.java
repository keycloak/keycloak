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
package org.keycloak.test.broker.saml;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper.Target;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;

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
    private static final String ATTRIBUTE_NAME = "urn:oid:0.9.2342.19200300.100.1.1";

    private final UsernameTemplateMapper mapper = new UsernameTemplateMapper();

    @Test
    public void resolvedTemplateSetsUsername() {
        BrokeredIdentityContext context = context("jdoe");

        mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${ALIAS}-${ATTRIBUTE.uid}"), context);

        assertEquals(ALIAS + "-jdoe", context.getModelUsername());
    }

    @Test
    public void missingAttributeIsRejected() {
        BrokeredIdentityContext context = context("jdoe");
        context.setModelUsername("previous-username");

        IdentityBrokerException e = assertThrows(IdentityBrokerException.class,
                () -> mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${ATTRIBUTE.non-existent}"), context));

        assertTrue(e.getMessage(), e.getMessage().contains("${ATTRIBUTE.non-existent}"));
        assertEquals("the existing username must not be overwritten", "previous-username", context.getModelUsername());
    }

    @Test
    public void partiallyMissingAttributeIsRejected() {
        BrokeredIdentityContext context = context("jdoe");

        assertThrows(IdentityBrokerException.class,
                () -> mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${ALIAS}-${ATTRIBUTE.non-existent}"), context));

        assertNull("an incomplete username must not be applied", context.getModelUsername());
    }

    @Test
    public void blankTemplateResultIsRejected() {
        for (String value : new String[]{"", "  "}) {
            BrokeredIdentityContext context = context(value);

            assertThrows(IdentityBrokerException.class,
                    () -> mapper.preprocessFederatedIdentity(null, null, mapperModel(Target.LOCAL, "${ATTRIBUTE.uid}"), context));

            assertNull("username [" + value + "] must not be applied", context.getModelUsername());
        }
    }

    @Test
    public void updateBrokeredUserNeverBlanksExistingUsername() {
        for (String username : new String[]{null, "", "  "}) {
            BrokeredIdentityContext context = context("jdoe");
            context.setModelUsername(username);
            List<String> written = new ArrayList<>();

            mapper.updateBrokeredUser(null, realm(false), user(written), mapperModel(Target.LOCAL, "${ALIAS}"), context);

            assertEquals("username [" + username + "] must not be written back", List.of(), written);
        }
    }

    @Test
    public void updateBrokeredUserKeepsUsernameWhenEmailIsUsedAsUsername() {
        BrokeredIdentityContext context = context("jdoe");
        context.setModelUsername(ALIAS + "-jdoe");
        List<String> written = new ArrayList<>();

        mapper.updateBrokeredUser(null, realm(true), user(written), mapperModel(Target.LOCAL, "${ALIAS}"), context);

        assertEquals(List.of(), written);
    }

    private BrokeredIdentityContext context(String attributeValue) {
        IdentityProviderModel idpConfig = new IdentityProviderModel();
        idpConfig.setAlias(ALIAS);
        idpConfig.setEnabled(true);

        AssertionType assertion = AssertionUtil.createAssertion("assertionId", NameIDType.deserializeFromString("nameIDType"));
        AttributeStatementType statement = new AttributeStatementType();
        assertion.addStatement(statement);
        AttributeType attribute = new AttributeType(ATTRIBUTE_NAME);
        attribute.setFriendlyName("uid");
        attribute.addAttributeValue(attributeValue);
        statement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));

        BrokeredIdentityContext context = new BrokeredIdentityContext("sub-1", idpConfig);
        context.getContextData().put(SAMLEndpoint.SAML_ASSERTION, assertion);
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