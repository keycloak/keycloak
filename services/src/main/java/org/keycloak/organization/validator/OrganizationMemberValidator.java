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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config.Scope;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationDomainModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.AbstractSimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

import static java.util.Optional.ofNullable;

import static org.keycloak.models.OrganizationDomainModel.ANY_DOMAIN;
import static org.keycloak.organization.utils.Organizations.resolveHomeBroker;
import static org.keycloak.validate.BuiltinValidators.emailValidator;

public class OrganizationMemberValidator extends AbstractSimpleValidator implements EnvironmentDependentProviderFactory {

    public static final String ID = "organization-member-validator";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(Object value, String inputHint, ValidationContext context, ValidatorConfig config) {
        KeycloakSession session = context.getSession();
        UserProfileAttributeValidationContext upContext = (UserProfileAttributeValidationContext) context;
        AttributeContext attributeContext = upContext.getAttributeContext();
        UserModel user = attributeContext.getUser();
        OrganizationModel organization = Organizations.resolveOrganization(session, user);

        // skip validation if we are not able to resolve org, or if user is not a member and the context is not IDP_REVIEW.
        if (organization == null || (user != null && !UserProfileContext.IDP_REVIEW.equals(attributeContext.getContext()) && !organization.isMember(user))) {
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
        if (!UserModel.EMAIL.equals(inputHint)) {
            return;
        }

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
        String emailDomain = email.substring(email.indexOf('@') + 1);
        Set<String> expectedDomains = organization.getDomains().map(OrganizationDomainModel::getName).collect(Collectors.toSet());

        if (expectedDomains.isEmpty()) {
            // no domain to check
            return;
        }

        if (UserProfileContext.IDP_REVIEW.equals(attributeContext.getContext())) {
            expectedDomains = resolveExpectedDomainsWhenReviewingFederatedUserProfile(organization, attributeContext);
        } else if (organization.isManaged(user)) {
            expectedDomains = resolveExpectedDomainsForManagedUser(organization, context, user);
        } else {
            // no validation happens for unmanaged users as they are realm users linked to an organization
            return;
        }

        if (expectedDomains.isEmpty() || expectedDomains.contains(emailDomain)) {
            // valid email domain
            return;
        }

        context.addError(new ValidationError(ID, inputHint, "Email domain does not match any domain from the organization"));
    }

    private static Set<String> resolveExpectedDomainsForManagedUser(OrganizationModel organization, ValidationContext context, UserModel user) {
        List<IdentityProviderModel> brokers = resolveHomeBroker(context.getSession(), user);

        if (brokers.isEmpty()) {
            return Set.of();
        }

        Set<String> domains = new HashSet<>();

        for (IdentityProviderModel broker : brokers) {
            String domain = broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
            if (ANY_DOMAIN.equals(domain)) {
                organization.getDomains().map(OrganizationDomainModel::getName).forEach(domains::add);
            }
            else if (domain != null) {
                domains.add(domain);
            }
        }

        return Collections.unmodifiableSet(domains);
    }

    private static Set<String> resolveExpectedDomainsWhenReviewingFederatedUserProfile(OrganizationModel organization, AttributeContext attributeContext) {
        // validating in the context of the brokering flow
        KeycloakSession session = attributeContext.getSession();
        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) session.getAttribute(BrokeredIdentityContext.class.getName());

        if (brokerContext == null) {
            return Set.of();
        }

        String alias = brokerContext.getIdpConfig().getAlias();
        IdentityProviderModel broker = organization.getIdentityProviders()
                .filter((p) -> p.getAlias().equals(alias))
                .findAny()
                .orElse(null);

        if (broker == null) {
            // the broker the user is authenticating is not linked to the organization
            return Set.of();
        }

        // expect the email domain to match the domain set to the broker or none if not set
        String brokerDomain = broker.getConfig().get(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE);
        if (ANY_DOMAIN.equals(brokerDomain)) {
            return organization.getDomains().map(OrganizationDomainModel::getName).collect(Collectors.toSet());
        }
        return  ofNullable(brokerDomain).map(Set::of).orElse(Set.of());
    }
}
