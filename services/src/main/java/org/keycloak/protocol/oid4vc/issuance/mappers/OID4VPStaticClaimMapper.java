package org.keycloak.protocol.oid4vc.issuance.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.OID4VCClientRegistrationProviderFactory;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows to add statically configured claims to the credential subject
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VPStaticClaimMapper extends OID4VPMapper {

    public static final String MAPPER_ID = "oid4vc-static-claim-mapper";

    public static final String SUBJECT_PROPERTY_CONFIG_KEY = "subjectProperty";
    public static final String STATIC_CLAIM_KEY = "staticValue";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    public OID4VPStaticClaimMapper() {
        super();
        ProviderConfigProperty subjectPropertyNameConfig = new ProviderConfigProperty();
        subjectPropertyNameConfig.setName(SUBJECT_PROPERTY_CONFIG_KEY);
        subjectPropertyNameConfig.setLabel("Static Claim Property Name");
        subjectPropertyNameConfig.setHelpText("Name of the property to contain the static value.");
        subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(subjectPropertyNameConfig);

        ProviderConfigProperty claimValueConfig = new ProviderConfigProperty();
        claimValueConfig.setName(STATIC_CLAIM_KEY);
        claimValueConfig.setLabel("Static Claim Value");
        claimValueConfig.setHelpText("Value to be set for the property.");
        // TODO: check how to allow multitypes in the future
        claimValueConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(claimValueConfig);
    }

    public static ProtocolMapperModel create(String mapperName, String propertyName, String value) {
        var mapperModel = new ProtocolMapperModel();
        mapperModel.setName(mapperName);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(SUBJECT_PROPERTY_CONFIG_KEY, propertyName);
        configMap.put(STATIC_CLAIM_KEY, value);
        mapperModel.setConfig(configMap);
        mapperModel.setProtocol(OID4VCClientRegistrationProviderFactory.PROTOCOL_ID);
        mapperModel.setProtocolMapper(MAPPER_ID);
        return mapperModel;
    }

    @Override protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        String propertyName = mapperModel.getConfig().get(SUBJECT_PROPERTY_CONFIG_KEY);
        String staticValue = mapperModel.getConfig().get(STATIC_CLAIM_KEY);
        claims.put(propertyName, staticValue);
    }

    @Override public String getDisplayType() {
        return "Static Claim Mapper";
    }

    @Override public String getHelpText() {
        return "Allows to set static values for the credential subject.";
    }

    @Override public String getId() {
        return MAPPER_ID;
    }
}