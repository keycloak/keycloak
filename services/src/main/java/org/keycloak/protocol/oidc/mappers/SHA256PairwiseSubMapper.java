package org.keycloak.protocol.oidc.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SHA256PairwiseSubMapper extends AbstractPairwiseSubMapper {
    public static final String PROVIDER_ID = "sha256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final Logger logger = Logger.getLogger(SHA256PairwiseSubMapper.class);
    private final Charset charset;

    public SHA256PairwiseSubMapper() {
        charset = Charset.forName("UTF-8");
    }

    public static ProtocolMapperRepresentation createPairwiseMapper(String sectorIdentifierUri, String salt) {
        Map<String, String> config;
        ProtocolMapperRepresentation pairwise = new ProtocolMapperRepresentation();
        pairwise.setName("pairwise subject identifier");
        pairwise.setProtocolMapper(new SHA256PairwiseSubMapper().getId());
        pairwise.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        config = new HashMap<>();
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
        if (salt == null) {
            salt = KeycloakModelUtils.generateId();
        }
        config.put(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT, salt);
        pairwise.setConfig(config);
        return pairwise;
    }

    @Override
    public void validateAdditionalConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        // Generate random salt if needed
        String salt = PairwiseSubMapperHelper.getSalt(mapperModel);
        if (salt == null || salt.trim().isEmpty()) {
            salt = generateSalt();
            PairwiseSubMapperHelper.setSalt(mapperModel, salt);
        }
    }

    @Override
    public String getHelpText() {
        return "Calculates a pairwise subject identifier using a salted sha-256 hash. See OpenID Connect specification for more info about pairwise subject identifiers.";
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(PairwiseSubMapperHelper.createSaltConfig());
        return configProperties;
    }

    @Override
    public String generateSub(ProtocolMapperModel mappingModel, String sectorIdentifier, String localSub) {
        String saltStr = PairwiseSubMapperHelper.getSalt(mappingModel);
        if (saltStr == null) {
            throw new IllegalStateException("Salt not available on mappingModel. Please update protocol mapper");
        }

        Charset charset = Charset.forName("UTF-8");
        byte[] salt = saltStr.getBytes(charset);
        String pairwiseSub = generateSub(sectorIdentifier, localSub, salt);
        logger.tracef("local sub = '%s', pairwise sub = '%s'", localSub, pairwiseSub);
        return pairwiseSub;
    }

    private String generateSub(String sectorIdentifier, String localSub, byte[] salt) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        sha256.update(sectorIdentifier.getBytes(charset));
        sha256.update(localSub.getBytes(charset));
        byte[] hash = sha256.digest(salt);
        return UUID.nameUUIDFromBytes(hash).toString();
    }

    private static String generateSalt() {
        return KeycloakModelUtils.generateId();
    }

    @Override
    public String getDisplayType() {
        return "Pairwise subject identifier";
    }

    @Override
    public String getIdPrefix() {
        return PROVIDER_ID;
    }
}