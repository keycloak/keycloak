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

package org.keycloak.models.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class StripSecretsUtilsTest {

    @Test
    public void checkStrippedRotatedSecret() {
        ClientRepresentation stripped = StripSecretsUtils.stripSecrets(null, createClient("unmasked_secret"));
        assertEquals(ComponentRepresentation.SECRET_VALUE, getRotatedSecret(stripped));
    }

    @Test
    public void checkStrippedRotatedSecretVaultUnaffected() {
        String rotatedSecret = "${vault.key}";
        ClientRepresentation stripped = StripSecretsUtils.stripSecrets(null, createClient(rotatedSecret));
        assertEquals(rotatedSecret, getRotatedSecret(stripped));
    }

    private ClientRepresentation createClient(String rotatedSecret) {
        ClientRepresentation client = new ClientRepresentation();
        Map<String, String> attrs = new HashMap<>();
        attrs.put(ClientSecretConstants.CLIENT_ROTATED_SECRET, rotatedSecret);
        client.setAttributes(attrs);
        return client;
    }

    private String getRotatedSecret(ClientRepresentation clientRepresentation) {
        return clientRepresentation.getAttributes().get(ClientSecretConstants.CLIENT_ROTATED_SECRET);
    }

    @Test
    public void stripUser() {
        UserRepresentation rep = new UserRepresentation();
        rep.setId("userId");
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType("password");
        credentialRepresentation.setSecretData("myPassword");
        rep.setCredentials(Arrays.asList(credentialRepresentation));
        rep.setEnabled(true);

        StripSecretsUtils.stripUser(rep);

        assertEquals("userId", rep.getId());
        assertNull(rep.getCredentials());
    }

    @Test
    public void stripClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setId("clientId");
        rep.setSecret("clientSecret");
        rep.setAttributes(new HashMap<>());
        rep.getAttributes().put("clientAttr1", "clientAttr1Value");
        rep.getAttributes().put("client.secret.rotated", "rotatedSecret");

        StripSecretsUtils.stripClient(rep);
        assertEquals("clientId", rep.getId());
        assertEquals("**********", rep.getSecret());
        assertEquals(2, rep.getAttributes().size());
        assertEquals("clientAttr1Value", rep.getAttributes().get("clientAttr1"));
        assertEquals("**********", rep.getAttributes().get("client.secret.rotated"));
    }
    @Test
    public void stripClientSecretsFromVault() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setId("clientId");
        rep.setSecret("${vault.clientSecret}");
        rep.setAttributes(new HashMap<>());
        rep.getAttributes().put("clientAttr1", "clientAttr1Value");
        rep.getAttributes().put("client.secret.rotated", "${vault.rotatedSecret}");

        StripSecretsUtils.stripClient(rep);
        assertEquals("clientId", rep.getId());
        assertEquals("${vault.clientSecret}", rep.getSecret());
        assertEquals(2, rep.getAttributes().size());
        assertEquals("clientAttr1Value", rep.getAttributes().get("clientAttr1"));
        assertEquals("${vault.rotatedSecret}", rep.getAttributes().get("client.secret.rotated"));
    }

    @Test
    public void stripBroker() {
        IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
        rep.setInternalId("brokerId");
        rep.setConfig(new HashMap<>());
        rep.getConfig().put("clientSecret", "secret");
        rep.getConfig().put("configParam1", "configValue1");

        StripSecretsUtils.stripBroker(rep);
        assertEquals("brokerId", rep.getInternalId());
        assertEquals(2, rep.getConfig().size());
        assertEquals("**********", rep.getConfig().get("clientSecret"));
        assertEquals("configValue1", rep.getConfig().get("configParam1"));
    }

    @Test
    public void stripCredentials(){
        CredentialRepresentation rep = new CredentialRepresentation();
        rep.setId("test");
        rep.setValue("secretValue");
        StripSecretsUtils.stripCredentials(rep);
        assertEquals("test", rep.getId());
        assertEquals("**********", rep.getValue());
    }

    @Test
    public void stripComponent() {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setId("componentId");
        rep.setName("componentName");
        rep.setProviderId("componentProviderId");
        rep.setProviderType("componentProviderType");
        rep.setParentId("componentParentId");
        rep.setSubType("componentSubType");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.put("secret", Arrays.asList("secretValue1", "secretValue2"));
        config.put("secretWithoutValues", Arrays.asList());
        config.put("nonSecret", Arrays.asList("nonSecretValue1", "nonSecretValue2"));
        rep.setConfig(config);

        Map<String, ProviderConfigProperty> configProperties = new HashMap<>();
        configProperties.put("secret", new ProviderConfigProperty("secret", "secretLabel", "secretHelpText", "secretType", "defaultValue", true));
        configProperties.put("secretWithoutValues", new ProviderConfigProperty("secretWithoutValues", "secretLabel", "secretHelpText", "secretType", "defaultValue", true));
        configProperties.put("nonSecret", new ProviderConfigProperty("nonSecret", "nonSecretLabel", "nonSecretHelpText", "secretType", "defaultValue", false));

        StripSecretsUtils.stripComponent(configProperties, rep);

        assertEquals("componentId", rep.getId());
        assertEquals("componentName", rep.getName());
        assertEquals(2, rep.getConfig().get("secret").size());
        assertEquals("**********", rep.getConfig().get("secret").get(0));
        assertEquals("**********", rep.getConfig().get("secret").get(1));
        assertEquals(1, rep.getConfig().get("secretWithoutValues").size());
        assertEquals("**********", rep.getConfig().get("secretWithoutValues").get(0));
        assertEquals(2, rep.getConfig().get("nonSecret").size());
        assertEquals("nonSecretValue1", rep.getConfig().get("nonSecret").get(0));
        assertEquals("nonSecretValue2", rep.getConfig().get("nonSecret").get(1));

    }

    @Test
    public void stripRealm() throws IOException {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("Master");
        rep.setId("realmId");

        rep.setSmtpServer(new HashMap<>());
        rep.getSmtpServer().put("password", "secret");
        rep.getSmtpServer().put("user", "smtpUser");

        ClientRepresentation client = new ClientRepresentation();
        client.setId("clientId");
        client.setSecret("clientSecret");
        client.setAttributes(new HashMap<>());
        client.getAttributes().put("clientAttr1", "clientAttr1Value");
        client.getAttributes().put("client.secret.rotated", "rotatedSecret");
        rep.setClients(Arrays.asList(client));

        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setProviderId("idpProviderId");
        idp.setAlias("idpAlias");
        idp.setConfig(new HashMap<>());
        idp.getConfig().put("idpConfig1", "idpConfig1Value");
        idp.getConfig().put("clientSecret", "ipdClientSecret");
        rep.setIdentityProviders(Arrays.asList(idp));

        UserRepresentation user = new UserRepresentation();
        user.setId("userId");
        user.setEnabled(true);
        CredentialRepresentation userCreds = new CredentialRepresentation();
        userCreds.setType(CredentialRepresentation.PASSWORD);
        userCreds.setValue("userPassword");
        user.setCredentials(Arrays.asList(userCreds));
        rep.setUsers(Arrays.asList(user));

        UserRepresentation fedUser = new UserRepresentation();
        fedUser.setId("fedUserId");
        fedUser.setEnabled(true);
        CredentialRepresentation fedUserCreds = new CredentialRepresentation();
        fedUserCreds.setType(CredentialRepresentation.PASSWORD);
        fedUserCreds.setValue("fedUserPassword");
        fedUser.setCredentials(Arrays.asList(fedUserCreds));
        rep.setFederatedUsers(Arrays.asList(fedUser));

        ComponentExportRepresentation component = new ComponentExportRepresentation();
        component.setId("componentId");
        component.setProviderId("componentProviderId");
        component.setConfig(new MultivaluedHashMap<>());
        component.getConfig().put("secret", Arrays.asList("secret1", "secret2"));
        component.getConfig().put("secretWithoutValues", Collections.emptyList());
        component.getConfig().put("nonSecret", Arrays.asList("nonSecret1", "nonSecret2"));

        rep.setComponents(new MultivaluedHashMap<>());
        rep.getComponents().put("componentExport", Arrays.asList(component));

        Map<String, ProviderConfigProperty> componentConfigProperties = new HashMap<>();
        componentConfigProperties.put("secret", new ProviderConfigProperty("secret", "secretLabel", "secretHelpText", "secretType", "defaultValue", true));
        componentConfigProperties.put("secretWithoutValues", new ProviderConfigProperty("secretWithoutValues", "secretLabel", "secretHelpText", "secretType", "defaultValue", true));
        componentConfigProperties.put("nonSecret", new ProviderConfigProperty("nonSecret", "nonSecretLabel", "nonSecretHelpText", "secretType", "defaultValue", false));
        StripSecretsUtils.GetComponentPropertiesFn fnGetComponentConfigProperties = (session, providerType, providerId) -> componentConfigProperties;

        StripSecretsUtils.stripRealm(null, rep, fnGetComponentConfigProperties);

        assertEquals("Master", rep.getRealm());
        assertEquals("realmId", rep.getId());
        assertEquals(2, rep.getSmtpServer().size());
        assertEquals("**********", rep.getSmtpServer().get("password"));
        assertEquals("smtpUser", rep.getSmtpServer().get("user"));

        assertEquals(1, rep.getClients().size());
        assertEquals("clientId", rep.getClients().get(0).getId());
        assertEquals("**********", rep.getClients().get(0).getSecret());
        assertEquals(2, rep.getClients().get(0).getAttributes().size());
        assertEquals("clientAttr1Value", rep.getClients().get(0).getAttributes().get("clientAttr1"));
        assertEquals("**********", rep.getClients().get(0).getAttributes().get("client.secret.rotated"));

        assertEquals(1, rep.getIdentityProviders().size());
        assertEquals(2, rep.getIdentityProviders().get(0).getConfig().size());
        assertEquals("idpConfig1Value", rep.getIdentityProviders().get(0).getConfig().get("idpConfig1"));
        assertEquals("**********", rep.getIdentityProviders().get(0).getConfig().get("clientSecret"));


        assertEquals(1, rep.getUsers().size());
        assertEquals("userId", rep.getUsers().get(0).getId());
        assertNull(rep.getUsers().get(0).getCredentials());

        assertEquals(1, rep.getFederatedUsers().size());
        assertEquals("fedUserId", rep.getFederatedUsers().get(0).getId());
        assertNull(rep.getFederatedUsers().get(0).getCredentials());

        assertEquals(1, rep.getComponents().size());
        assertEquals(1, rep.getComponents().get("componentExport").size());
        MultivaluedHashMap<String, String> componentExportConfig = rep.getComponents().get("componentExport").get(0).getConfig();
        assertNotNull(componentExportConfig);
        assertEquals(2, componentExportConfig.get("secret").size());
        assertEquals("**********", componentExportConfig.get("secret").get(0));
        assertEquals("**********", componentExportConfig.get("secret").get(1));
        assertEquals(1, componentExportConfig.get("secretWithoutValues").size());
        assertEquals("**********", componentExportConfig.get("secretWithoutValues").get(0));
        assertEquals(2, componentExportConfig.get("nonSecret").size());
        assertEquals("nonSecret1", componentExportConfig.get("nonSecret").get(0));
        assertEquals("nonSecret2", componentExportConfig.get("nonSecret").get(1));
    }

}
