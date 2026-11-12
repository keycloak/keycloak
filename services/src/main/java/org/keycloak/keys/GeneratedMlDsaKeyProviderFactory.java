package org.keycloak.keys;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import org.jboss.logging.Logger;

public class GeneratedMlDsaKeyProviderFactory implements KeyProviderFactory<GeneratedMlDsaKeyProvider>, EnvironmentDependentProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedMlDsaKeyProviderFactory.class);

    public static final String ID = "mldsa-generated";

    private static final String HELP_TEXT = "Generates ML-DSA keys";

    private static final String DEFAULT_ALGORITHM = Algorithm.ML_DSA_65;

    public static final Set<String> SUPPORTED_ALGORITHMS = Set.of(
            Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87);

    private static final ProviderConfigProperty ALGORITHM_PROPERTY = new ProviderConfigProperty(Attributes.ALGORITHM_KEY, "Algorithm", "Intended algorithm for the key", ProviderConfigProperty.LIST_TYPE, DEFAULT_ALGORITHM, new LinkedList<>(SUPPORTED_ALGORITHMS));

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property(Attributes.PRIORITY_PROPERTY)
            .property(Attributes.ENABLED_PROPERTY)
            .property(Attributes.ACTIVE_PROPERTY)
            .property(ALGORITHM_PROPERTY)
            .build();

    @Override
    public GeneratedMlDsaKeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedMlDsaKeyProvider(model);
    }

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (keyUse.equals(KeyUse.SIG) && SUPPORTED_ALGORITHMS.contains(algorithm)) {
            RealmModel realm = session.getContext().getRealm();

            ComponentModel generated = new ComponentModel();
            generated.setName("fallback-" + algorithm);
            generated.setParentId(realm.getId());
            generated.setProviderId(ID);
            generated.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle(Attributes.PRIORITY_KEY, "-100");
            config.putSingle(Attributes.ALGORITHM_KEY, algorithm);
            generated.setConfig(config);

            realm.addComponentModel(generated);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        if (model.getId() != null) {
            ComponentModel current = realm.getComponent(model.getId());
            if (current != null) {
                if (!current.get(Attributes.ALGORITHM_KEY).equals(model.get(Attributes.ALGORITHM_KEY))) {
                    throw new ComponentValidationException("Changing algorithm is not supported");
                }
            }
        }

        ConfigurationValidationHelper.check(model)
                .checkLong(Attributes.PRIORITY_PROPERTY, false)
                .checkBoolean(Attributes.ENABLED_PROPERTY, false)
                .checkBoolean(Attributes.ACTIVE_PROPERTY, false)
                .checkList(ALGORITHM_PROPERTY, false);

        String algorithm = model.get(Attributes.ALGORITHM_KEY);
        if (algorithm == null) {
            algorithm = DEFAULT_ALGORITHM;
            model.put(Attributes.ALGORITHM_KEY, algorithm);
        }

        if (!(model.contains(Attributes.PRIVATE_KEY_KEY) && model.contains(Attributes.PUBLIC_KEY_KEY))) {
            generateKeys(model, algorithm);
            logger.debugv("Generated keys for {0}", realm.getName());
        }
    }

    private static void generateKeys(ComponentModel model, String algorithm) {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
            keyPair = keyGen.generateKeyPair();
            model.put(Attributes.PRIVATE_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            model.put(Attributes.PUBLIC_KEY_KEY, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        } catch (Throwable t) {
            throw new ComponentValidationException("Failed to generate ML-DSA keys", t);
        }
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OAUTH_ML_DSA);
    }

}
