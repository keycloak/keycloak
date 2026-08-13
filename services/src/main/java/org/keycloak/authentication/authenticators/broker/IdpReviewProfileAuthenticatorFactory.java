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

package org.keycloak.authentication.authenticators.broker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpReviewProfileAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "idp-review-profile";
    static IdpReviewProfileAuthenticator SINGLETON = new IdpReviewProfileAuthenticator();

    public static final String UPDATE_PROFILE_ON_FIRST_LOGIN = "update.profile.on.first.login";

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "reviewProfile";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "Review Profile";
    }

    @Override
    public String getHelpText() {
        return "User reviews and updates profile data retrieved from Identity Provider in the displayed form";
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(UPDATE_PROFILE_ON_FIRST_LOGIN);
        property.setLabel("Update Profile on First Login");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        List<String> updateProfileValues = Arrays.asList(IdentityProviderRepresentation.UPFLM_ON, IdentityProviderRepresentation.UPFLM_MISSING, IdentityProviderRepresentation.UPFLM_OFF);
        property.setOptions(updateProfileValues);
        property.setDefaultValue(IdentityProviderRepresentation.UPFLM_MISSING);
        property.setHelpText("Define conditions under which a user has to review and update his profile after first-time login. Value 'On' means that"
                + " page for reviewing profile will be displayed and user can review and update his profile. Value 'off' means that page won't be displayed."
                + " Value 'missing' means that page is displayed just when some required attribute is missing (wasn't downloaded from identity provider). Value 'missing' is the default one."
                + " WARN: In case that user clicks 'Review profile info' on link duplications page, the update page will be always displayed. You would need to disable this authenticator to never display the page.");

        configProperties.add(property);
    }


    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
