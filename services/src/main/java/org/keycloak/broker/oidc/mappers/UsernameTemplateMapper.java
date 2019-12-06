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

package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.social.bitbucket.BitbucketIdentityProviderFactory;
import org.keycloak.social.facebook.FacebookIdentityProviderFactory;
import org.keycloak.social.github.GitHubIdentityProviderFactory;
import org.keycloak.social.gitlab.GitLabIdentityProviderFactory;
import org.keycloak.social.google.GoogleIdentityProviderFactory;
import org.keycloak.social.instagram.InstagramIdentityProviderFactory;
import org.keycloak.social.linkedin.LinkedInIdentityProviderFactory;
import org.keycloak.social.microsoft.MicrosoftIdentityProviderFactory;
import org.keycloak.social.openshift.OpenshiftV3IdentityProviderFactory;
import org.keycloak.social.openshift.OpenshiftV4IdentityProviderFactory;
import org.keycloak.social.paypal.PayPalIdentityProviderFactory;
import org.keycloak.social.stackoverflow.StackoverflowIdentityProviderFactory;
import org.keycloak.social.twitter.TwitterIdentityProviderFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsernameTemplateMapper extends AbstractClaimMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID,
            BitbucketIdentityProviderFactory.PROVIDER_ID,
            FacebookIdentityProviderFactory.PROVIDER_ID,
            GitHubIdentityProviderFactory.PROVIDER_ID,
            GitLabIdentityProviderFactory.PROVIDER_ID,
            GoogleIdentityProviderFactory.PROVIDER_ID,
            InstagramIdentityProviderFactory.PROVIDER_ID,
            LinkedInIdentityProviderFactory.PROVIDER_ID,
            MicrosoftIdentityProviderFactory.PROVIDER_ID,
            OpenshiftV3IdentityProviderFactory.PROVIDER_ID,
            OpenshiftV4IdentityProviderFactory.PROVIDER_ID,
            PayPalIdentityProviderFactory.PROVIDER_ID,
            StackoverflowIdentityProviderFactory.PROVIDER_ID,
            TwitterIdentityProviderFactory.PROVIDER_ID
    };

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String TEMPLATE = "template";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TEMPLATE);
        property.setLabel("Template");
        property.setHelpText("Template to use to format the username to import.  Substitutions are enclosed in ${}.  For example: '${ALIAS}.${CLAIM.sub}'.  ALIAS is the provider alias.  CLAIM.<NAME> references an ID or Access token claim.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${ALIAS}.${CLAIM.preferred_username}");
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-username-idp-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Preprocessor";
    }

    @Override
    public String getDisplayType() {
        return "Username Template Importer";
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
    }

    static Pattern substitution = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String template = mapperModel.getConfig().get(TEMPLATE);
        Matcher m = substitution.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            if (variable.equals("ALIAS")) {
                m.appendReplacement(sb, context.getIdpConfig().getAlias());
            } else if (variable.equals("UUID")) {
                m.appendReplacement(sb, KeycloakModelUtils.generateId());
            } else if (variable.startsWith("CLAIM.")) {
                String name = variable.substring("CLAIM.".length());
                Object value = AbstractClaimMapper.getClaimValue(context, name);
                if (value == null) value = "";
                m.appendReplacement(sb, value.toString());
            } else {
                m.appendReplacement(sb, m.group(1));
            }

        }
        m.appendTail(sb);
        String username = sb.toString();
        context.setModelUsername(username);

    }

    @Override
    public String getHelpText() {
        return "Format the username to import.";
    }
}
