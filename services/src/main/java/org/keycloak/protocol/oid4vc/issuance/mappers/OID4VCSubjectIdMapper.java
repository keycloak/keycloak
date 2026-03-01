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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;

/**
 * Sets an ID for the credential subject, either from User ID or by attribute mapping
 * <p/>
 * A Verifiable Credential (VC) is often bound to a Holder's Digital Identity (DID).
 * The Holder's DID is in turn bound to Key Material that the Issuer + Verifier can discover from the DID Document.
 * In case of "did:key:..." the Public Key is already encoded in the DID.
 * <p/>
 * Here, we make sure that the default UserProfile has a 'did' attribute when --feature=oid4vc-vci is enabled.
 * Conceptually, it is however debatable whether the Issuer should know even one of the Holder's DIDs
 * <p/>
 * In future, it may be possible that ...
 * <p/>
 *  * The Holder communicates the DID at the time of Authorization
 *  * The Issuer then verifies Holder possession of the associated Key Material
 *  * The Issuer then somehow associates the AuthorizationRequest with a registered User
 *  * The VC is then issued to the Holder without the Issuer needing to remember that Holder DID
 * <p/>
 * This kind of Authorization protocol is for example required by EBSI, which we aim to become compatible with.
 * https:*hub.ebsi.eu/conformance/build-solutions/issue-to-holder-functional-flows
 * <p/>
 * Note, that current EBSI Compatibility Tests use the Holder's DID as OIDC client_id in the AuthorizationRequest.
 * That is something we need to work with, but I don't think we should model it like that in our realm config.
 * <p/>
 * Here the attribute definition that we add by default (when not defined already)
 * <p/>
 *   {
 *     "name": "did",
 *     "displayName": "DID",
 *     "permissions": {
 *       "view": ["admin", "user"],
 *       "edit": ["admin", "user"]
 *     },
 *     "validations": {
 *       "pattern": {
 *         "pattern": "^did:.+:.+$",
 *         "error-message": "Value must start with 'did:scheme:'"
 *       }
 *     }
 *   }
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCSubjectIdMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-subject-id-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty idPropertyNameConfig = new ProviderConfigProperty();
        idPropertyNameConfig.setName(CLAIM_NAME);
        idPropertyNameConfig.setLabel(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_LABEL);
        idPropertyNameConfig.setHelpText("Name of the claim to insert into the token. This can be a fully qualified name such as 'address.street'. In case that 'id' is used as a value of this configuration property, it would be mapped into sd-jwt credential as claim 'sub'.");
        idPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        idPropertyNameConfig.setDefaultValue(CLAIM_NAME_SUBJECT_ID);
        CONFIG_PROPERTIES.add(idPropertyNameConfig);

        ProviderConfigProperty userAttributeConfig = new ProviderConfigProperty();
        userAttributeConfig.setName(USER_ATTRIBUTE_KEY);
        userAttributeConfig.setLabel("User attribute");
        userAttributeConfig.setHelpText("The name of the user attribute that maps to the subject id.");
        userAttributeConfig.setType(ProviderConfigProperty.LIST_TYPE);
        userAttributeConfig.setOptions(List.of(UserModel.DID, UserModel.USERNAME, UserModel.EMAIL, UserModel.ID));
        userAttributeConfig.setDefaultValue(UserModel.DID);
        CONFIG_PROPERTIES.add(userAttributeConfig);
    }

    public static ProtocolMapperModel create(String name, String claimName, String userAttribute) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(name);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLAIM_NAME, claimName);
        configMap.put(USER_ATTRIBUTE_KEY, userAttribute);
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public void setClaim(VerifiableCredential verifiableCredential, UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaim(Map<String, Object> claims, UserSessionModel userSessionModel) {
        UserModel userModel = userSessionModel.getUser();
        List<String> attributePath = getMetadataAttributePath();
        String propertyName = attributePath.get(attributePath.size() - 1);
        String userAttributeName = mapperModel.getConfig().get(OID4VCMapper.USER_ATTRIBUTE_KEY);
        Consumer<String> userIdConsumer = (val) -> claims.put(propertyName, val);
        if (UserModel.ID.equals(userAttributeName)) {
            userIdConsumer.accept(userModel.getId());
        } else {
            KeycloakModelUtils.resolveAttribute(userModel, userAttributeName, false).stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .ifPresent(userIdConsumer);
        }
    }

    @Override
    public String getDisplayType() {
        return "CredentialSubject ID Mapper";
    }

    @Override
    public String getHelpText() {
        return "Sets an ID for the credential subject, either from User ID or by attribute mapping.";
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCSubjectIdMapper();
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }
}
