package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

public class PairwiseSubMapperHelper {

    public static final String SECTOR_IDENTIFIER_URI = "sectorIdentifierUri";
    public static final String SECTOR_IDENTIFIER_URI_LABEL = "sectorIdentifierUri.label";
    public static final String SECTOR_IDENTIFIER_URI_HELP_TEXT = "sectorIdentifierUri.tooltip";

    public static final String PAIRWISE_SUB_ALGORITHM_SALT = "pairwiseSubAlgorithmSalt";
    public static final String PAIRWISE_SUB_ALGORITHM_SALT_LABEL = "pairwiseSubAlgorithmSalt.label";
    public static final String PAIRWISE_SUB_ALGORITHM_SALT_HELP_TEXT = "pairwiseSubAlgorithmSalt.tooltip";

    public static String getSectorIdentifierUri(ProtocolMapperRepresentation mappingModel) {
        return mappingModel.getConfig().get(SECTOR_IDENTIFIER_URI);
    }

    public static void setSectorIdentifierUri(ProtocolMapperModel mappingModel, String sectorIdentifierUri) {
        mappingModel.getConfig().put(SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
    }

    public static String getSectorIdentifierUri(ProtocolMapperModel mappingModel) {
        return mappingModel.getConfig().get(SECTOR_IDENTIFIER_URI);
    }

    public static String getSalt(ProtocolMapperModel mappingModel) {
        return mappingModel.getConfig().get(PAIRWISE_SUB_ALGORITHM_SALT);
    }

    public static void setSalt(ProtocolMapperModel mappingModel, String salt) {
        mappingModel.getConfig().put(PAIRWISE_SUB_ALGORITHM_SALT, salt);
    }

    public static ProviderConfigProperty createSectorIdentifierConfig() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(SECTOR_IDENTIFIER_URI);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(SECTOR_IDENTIFIER_URI_LABEL);
        property.setHelpText(SECTOR_IDENTIFIER_URI_HELP_TEXT);
        return property;
    }

    public static ProviderConfigProperty createSaltConfig() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(PAIRWISE_SUB_ALGORITHM_SALT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(PAIRWISE_SUB_ALGORITHM_SALT_LABEL);
        property.setHelpText(PAIRWISE_SUB_ALGORITHM_SALT_HELP_TEXT);
        return property;
    }
}
