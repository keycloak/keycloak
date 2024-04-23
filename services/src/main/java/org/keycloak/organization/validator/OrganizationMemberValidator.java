/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.organization.validator;

import static org.keycloak.validate.BuiltinValidators.emailValidator;

import java.util.stream.Stream;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.AbstractSimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

public class OrganizationMemberValidator extends AbstractSimpleValidator implements EnvironmentDependentProviderFactory {

    public static final String ID = "organization-member-validator";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(Object value, String inputHint, ValidationContext context, ValidatorConfig config) {
        KeycloakSession session = context.getSession();
        OrganizationModel organization = resolveOrganization(context, session);

        if (organization == null) {
            return;
        }

        validateEmailDomain((String) value, inputHint, context, organization);
    }

    @Override
    protected boolean skipValidation(Object value, ValidatorConfig config) {
        return false;
    }

    @Override
    public boolean isSupported(Scope config) {
        return Profile.isFeatureEnabled(Feature.ORGANIZATION);
    }

    private void validateEmailDomain(String email, String inputHint, ValidationContext context, OrganizationModel organization) {
        if (UserModel.EMAIL.equals(inputHint)) {
            if (StringUtil.isBlank(email)) {
                context.addError(new ValidationError(ID, inputHint, "Email not set"));
                return;
            }

            if (!emailValidator().validate(email, inputHint, context).isValid()) {
                return;
            }

            UserProfileAttributeValidationContext upContext = (UserProfileAttributeValidationContext) context;
            AttributeContext attributeContext = upContext.getAttributeContext();
            UserModel user = attributeContext.getUser();

            if (!organization.isManaged(user)) {
                return;
            }

            String domain = email.substring(email.indexOf('@') + 1);
            Stream<OrganizationDomainModel> expectedDomains = organization.getDomains();

            if (expectedDomains.map(OrganizationDomainModel::getName).noneMatch(domain::equals)) {
                context.addError(new ValidationError(ID, inputHint, "Email domain does not match any domain from the organization"));
            }
        }
    }

    private OrganizationModel resolveOrganization(ValidationContext context, KeycloakSession session) {
        OrganizationModel organization = (OrganizationModel) session.getAttribute(OrganizationModel.class.getName());

        if (organization != null) {
            return organization;
        }

        UserProfileAttributeValidationContext upContext = (UserProfileAttributeValidationContext) context;
        AttributeContext attributeContext = upContext.getAttributeContext();
        UserModel user = attributeContext.getUser();

        if (user != null) {
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            return provider.getByMember(user);
        }

        return null;
    }
}
