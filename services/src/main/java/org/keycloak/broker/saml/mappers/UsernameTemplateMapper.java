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

package org.keycloak.broker.saml.mappers;

import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UsernameTemplateMapper extends AbstractIdentityProviderMapper {

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    public static final String TEMPLATE = "template";
    public static final String TARGET = "target";

    public enum Target  {
        LOCAL              { public void set(BrokeredIdentityContext context, String value) { context.setModelUsername(value); } },
        BROKER_ID          { public void set(BrokeredIdentityContext context, String value) { context.setId(value); } },
        BROKER_USERNAME    { public void set(BrokeredIdentityContext context, String value) { context.setUsername(value); } };
        public abstract void set(BrokeredIdentityContext context, String value);
    }
    public static final List<String> TARGETS = Arrays.asList(Target.LOCAL.toString(), Target.BROKER_ID.toString(), Target.BROKER_USERNAME.toString());

    public static final Map<String, UnaryOperator<String>> TRANSFORMERS = new HashMap<>();

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TEMPLATE);
        property.setLabel("Template");
        property.setHelpText("Template to use to format the username to import.  Substitutions are enclosed in ${}.  For example: '${ALIAS}.${NAMEID}'.  ALIAS is the provider alias.  NAMEID is that SAML name id assertion.  ATTRIBUTE.<NAME> references a SAML attribute where name is the attribute name or friendly name. \n"
          + "The substitution can be converted to upper or lower case by appending |uppercase or |lowercase to the substituted value, e.g. '${NAMEID | lowercase} \n"
          + "Local part of email can be extracted by appending |localpart to the substituted value, e.g. ${CLAIM.email | localpart}. If \"@\" is not part of the string, this conversion leaves the substitution untouched.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("${ALIAS}.${NAMEID}");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TARGET);
        property.setLabel("Target");
        property.setHelpText("Destination field for the mapper. LOCAL (default) means that the changes are applied to the username stored in local database upon user import. BROKER_ID and BROKER_USERNAME means that the changes are stored into the ID or username used for federation user lookup, respectively.");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(TARGETS);
        property.setDefaultValue(Target.LOCAL.toString());
        configProperties.add(property);

        TRANSFORMERS.put("uppercase", String::toUpperCase);
        TRANSFORMERS.put("lowercase", String::toLowerCase);
        TRANSFORMERS.put("localpart", UsernameTemplateMapper::getEmailLocalPart);
    }

    public static final String PROVIDER_ID = "saml-username-idp-mapper";

    public static String getEmailLocalPart(String email) {
        int index = email == null ? -1 : email.lastIndexOf('@');
        if (index >= 0) {
            return email.substring(0, index);
        } else {
            return email;
        }
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

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
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        // preprocessFederatedIdentity gets called anyways, so we only need to set the username if necessary.
        // However, we don't want to set the username when the email is used as username
        if (getTarget(mapperModel.getConfig().get(TARGET)) == Target.LOCAL && !realm.isRegistrationEmailAsUsername()) {
            user.setUsername(context.getModelUsername());
        }
    }

    private static final Pattern SUBSTITUTION = Pattern.compile("\\$\\{([^}]+?)(?:\\s*\\|\\s*(\\S+)\\s*)?\\}");

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        setUserNameFromTemplate(mapperModel, context);
    }

    private void setUserNameFromTemplate(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        String template = mapperModel.getConfig().get(TEMPLATE);
        Matcher m = SUBSTITUTION.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String variable = m.group(1);
            UnaryOperator<String> transformer = Optional.ofNullable(m.group(2)).map(TRANSFORMERS::get).orElse(UnaryOperator.identity());

            if (variable.equals("ALIAS")) {
                m.appendReplacement(sb, transformer.apply(context.getIdpConfig().getAlias()));
            } else if (variable.equals("UUID")) {
                m.appendReplacement(sb, transformer.apply(KeycloakModelUtils.generateId()));
            } else if (variable.equals("NAMEID")) {
                SubjectType subject = assertion.getSubject();
                SubjectType.STSubType subType = subject.getSubType();
                NameIDType subjectNameID = (NameIDType) subType.getBaseID();
                m.appendReplacement(sb, transformer.apply(subjectNameID.getValue()));
            } else if (variable.startsWith("ATTRIBUTE.")) {
                String name = variable.substring("ATTRIBUTE.".length());
                String value = "";
                for (AttributeStatementType statement : assertion.getAttributeStatements()) {
                    for (AttributeStatementType.ASTChoiceType choice : statement.getAttributes()) {
                        AttributeType attr = choice.getAttribute();
                        if (name.equals(attr.getName()) || name.equals(attr.getFriendlyName())) {
                            List<Object> attributeValue = attr.getAttributeValue();
                            if (attributeValue != null && !attributeValue.isEmpty()) {
                                value = attributeValue.get(0).toString();
                            }
                            break;
                        }
                    }
                }
                m.appendReplacement(sb, transformer.apply(value));
            } else {
                m.appendReplacement(sb, m.group(1));
            }

        }
        m.appendTail(sb);

        Target t = getTarget(mapperModel.getConfig().get(TARGET));
        t.set(context, sb.toString());
    }

    @Override
    public String getHelpText() {
        return "Format the username to import.";
    }

    public static Target getTarget(String value) {
        try {
            return value == null ? Target.LOCAL : Target.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return Target.LOCAL;
        }
    }

}
