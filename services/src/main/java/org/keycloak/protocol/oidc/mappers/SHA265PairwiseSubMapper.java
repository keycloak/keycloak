package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ServicesLogger;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class SHA265PairwiseSubMapper extends AbstractPairwiseSubMapper {
    public static final String PROVIDER_ID = "sha256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String ALPHA_NUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    private final Charset charset;

    public SHA265PairwiseSubMapper() throws NoSuchAlgorithmException {
        charset = Charset.forName("UTF-8");
        MessageDigest.getInstance(HASH_ALGORITHM);
    }

    public static ProtocolMapperModel createPairwiseMapper() {
        return createPairwiseMapper(null);
    }

    public static ProtocolMapperModel createPairwiseMapper(String sectorIdentifierUri) {
        Map<String, String> config;
        ProtocolMapperModel pairwise = new ProtocolMapperModel();
        pairwise.setName("pairwise subject identifier");
        pairwise.setProtocolMapper(AbstractPairwiseSubMapper.getId(PROVIDER_ID));
        pairwise.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        pairwise.setConsentRequired(false);
        config = new HashMap<>();
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
        pairwise.setConfig(config);
        return pairwise;
    }

    public static ProtocolMapperModel createPairwiseMapper(String sectorIdentifierUri, String salt) {
        Map<String, String> config;
        ProtocolMapperModel pairwise = new ProtocolMapperModel();
        pairwise.setName("pairwise subject identifier");
        pairwise.setProtocolMapper(AbstractPairwiseSubMapper.getId(PROVIDER_ID));
        pairwise.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        pairwise.setConsentRequired(false);
        config = new HashMap<>();
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
        config.put(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT, salt);
        pairwise.setConfig(config);
        return pairwise;
    }

    @Override
    public String getHelpText() {
        return "Calculates a pairwise subject identifier using a salted sha-256 hash.";
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(PairwiseSubMapperHelper.createSaltConfig());
        return configProperties;
    }

    @Override
    public String generateSub(ProtocolMapperModel mappingModel, String sectorIdentifier, String localSub) {
        String saltStr = getSalt(mappingModel);

        Charset charset = Charset.forName("UTF-8");
        byte[] salt = saltStr.getBytes(charset);
        String pairwiseSub = generateSub(sectorIdentifier, localSub, salt);
        logger.infof("local sub = '%s', pairwise sub = '%s'", localSub, pairwiseSub);
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

    private String getSalt(ProtocolMapperModel mappingModel) {
        String salt = PairwiseSubMapperHelper.getSalt(mappingModel);
        if (salt == null || salt.trim().isEmpty()) {
            salt = createSalt(32);
            PairwiseSubMapperHelper.setSalt(mappingModel, salt);
        }
        return salt;
    }

    private String createSalt(int len) {
        Random rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(ALPHA_NUMERIC.charAt(rnd.nextInt(ALPHA_NUMERIC.length())));
        return sb.toString();
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
