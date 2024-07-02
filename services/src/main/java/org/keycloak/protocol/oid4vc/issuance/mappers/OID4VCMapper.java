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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oid4vc.OID4VCLoginProtocolFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Base class for OID4VC Mappers, to provide common configuration and functionality for all of them
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public abstract class OID4VCMapper implements ProtocolMapper, OID4VCEnvironmentProviderFactory {

    protected static final String SUPPORTED_CREDENTIALS_KEY = "supportedCredentialTypes";

    protected ProtocolMapperModel mapperModel;

    private static final List<ProviderConfigProperty> OID4VC_CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty supportedCredentialsConfig = new ProviderConfigProperty();
        supportedCredentialsConfig.setType(ProviderConfigProperty.STRING_TYPE);
        supportedCredentialsConfig.setLabel("Supported Credential Types");
        supportedCredentialsConfig.setDefaultValue("VerifiableCredential");
        supportedCredentialsConfig.setHelpText(
                "Types of Credentials to apply the mapper. Needs to be a comma-separated list.");
        supportedCredentialsConfig.setName(SUPPORTED_CREDENTIALS_KEY);
        OID4VC_CONFIG_PROPERTIES.clear();
        OID4VC_CONFIG_PROPERTIES.add(supportedCredentialsConfig);
    }

    protected abstract List<ProviderConfigProperty> getIndividualConfigProperties();

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Stream.concat(OID4VC_CONFIG_PROPERTIES.stream(), getIndividualConfigProperties().stream()).toList();
    }

    public OID4VCMapper setMapperModel(ProtocolMapperModel mapperModel) {
        this.mapperModel = mapperModel;
        return this;
    }

    @Override
    public String getProtocol() {
        return OID4VCLoginProtocolFactory.PROTOCOL_ID;
    }

    @Override
    public String getDisplayCategory() {
        return "OID4VC Mapper";
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // try to get the credentials
    }

    @Override
    public void close() {
    }

    /**
     * Checks if the mapper supports the given credential type. Allows to configure them not only per client, but also per VC Type.
     *
     * @param credentialScope type of the VerifiableCredential that should be checked
     * @return true if it is supported
     */
    public boolean isScopeSupported(String credentialScope) {
        var optionalScopes = Optional.ofNullable(mapperModel.getConfig().get(SUPPORTED_CREDENTIALS_KEY));
        if (optionalScopes.isEmpty()) {
            return false;
        }
        return Arrays.asList(optionalScopes.get().split(",")).contains(credentialScope);
    }

    /**
     * Set the claims to credential, like f.e. the context
     */
    public abstract void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                                UserSessionModel userSessionModel);

    /**
     * Set the claims to the credential subject.
     */
    public abstract void setClaimsForSubject(Map<String, Object> claims,
                                             UserSessionModel userSessionModel);

}
