/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.tests.providers;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.directgrant.ValidateOTP;
import org.keycloak.authentication.authenticators.directgrant.ValidatePassword;
import org.keycloak.authentication.authenticators.directgrant.ValidateUsername;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.provider.Provider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.providersoverride.CustomDefaultEmailSenderProvider2;
import org.keycloak.tests.providers.providersoverride.CustomLoginFormsProvider;
import org.keycloak.tests.providers.providersoverride.CustomValidatePassword2;
import org.keycloak.tests.providers.providersoverride.CustomValidateUsername;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for having multiple providerFactory of same SPI with same providerId
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class ProvidersOverrideTest {

    @InjectRealm(attachTo = "master", ref = "master")
    ManagedRealm masterRealm;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    RunOnServerClient runOnServerMaster;

    @Test
    public void testBuiltinAuthenticatorsOverride() {
        // The custom provider would be preferred over the internal ValidateUsername. Both has same order, so custom provider would be chosen (backwards compatibility with previous versions)
        testProviderImplementationClass(Authenticator.class, ValidateUsername.PROVIDER_ID, CustomValidateUsername.class);

        // The provider with highest order is chosen
        testProviderImplementationClass(Authenticator.class, ValidatePassword.PROVIDER_ID, CustomValidatePassword2.class);

        // The builtin ValidateOTP class is chosen as it has higher order than the CustomValidateOTP
        testProviderImplementationClass(Authenticator.class, ValidateOTP.PROVIDER_ID, ValidateOTP.class);
    }

    @Test
    public void testDefaultProvidersOverride() {
        // The custom provider would be preferred over the internal FreemarkerLoginFormsProvider. Both has same order, so custom provider would be chosen (backwards compatibility with previous versions)
        testProviderImplementationClass(LoginFormsProvider.class, null, CustomLoginFormsProvider.class);

        // The provider with highest order is chosen
        testProviderImplementationClass(EmailSenderProvider.class, null, CustomDefaultEmailSenderProvider2.class);
    }

    private void testProviderImplementationClass(Class<? extends Provider> providerClass, String providerId, Class<? extends Provider> expectedProviderImplClass) {
        String providerImplClass = runOnServerMaster.fetchString(getProviderClassName(providerClass.getName(), providerId)).replaceAll("\"", "");
        Assertions.assertEquals(expectedProviderImplClass.getName(), providerImplClass);
    }

    /**
     * @param providerClass Full name of class such as for example "org.keycloak.authentication.Authenticator"
     * @param providerId    providerId referenced in particular provider factory. Can be null (in this case we're returning default provider for particular providerClass)
     * @return fullname of provider implementation class
     */
    private static FetchOnServer getProviderClassName(String providerClass, String providerId) {
        return session -> {
            try {
                Class<? extends Provider> providerClazz = (Class<? extends Provider>) Class.forName(providerClass);
                Provider provider = (providerId == null) ? session.getProvider(providerClazz) : session.getProvider(providerClazz, providerId);
                return provider.getClass().getName();
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Cannot find provider class: " + providerClass, cnfe);
            }
        };
    }
}
