package org.keycloak.operator.v2alpha1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.realm.Realm;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import static org.keycloak.operator.Constants.MANAGED_BY_LABEL;
import static org.keycloak.operator.Constants.MANAGED_BY_VALUE;

public class RealmSecretProvider {

    private final static Logger logger = Logger.getLogger(RealmSecretProvider.class);

    private KubernetesClient client;
    private ObjectMapper jsonMapper;
    private Base64.Decoder base64Decoder = Base64.getDecoder();
    private Base64.Encoder base64Encoder = Base64.getEncoder();

    public RealmSecretProvider(KubernetesClient client, ObjectMapper jsonMapper) {
        this.client = client;
        this.jsonMapper = jsonMapper;
    }

    public static String getSecretName(Deployment deployment, Realm realm) {
        return KubernetesResourceUtil.sanitizeName(deployment.getMetadata().getName() + "-" + realm.getSpec().getRealm().getRealm() + "-realm");
    }

    private Secret buildSecret(String name, String namespace, String fileName, String content, List<OwnerReference> ownerReferences) {
        return new SecretBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(MANAGED_BY_LABEL, MANAGED_BY_VALUE)
                .withOwnerReferences(ownerReferences)
                .endMetadata()
                .addToStringData(fileName, content)
                .build();
    }

    public void handleRealmSecret(String secretName, Realm realm, List<OwnerReference> ownerReferences) {
        // Write the realm representation secret
        var realmName = realm.getSpec().getRealm().getRealm();
        var namespace = realm.getMetadata().getNamespace();
        var fileName = realmName + "-realm.json";

        var content = "";
        try {
            content = jsonMapper.writeValueAsString(realm.getSpec().getRealm());
        } catch (JsonProcessingException cause) {
            throw new RuntimeException("Failed to read the Realm Representation", cause);
        }

        var secretSelector = client
                .secrets()
                .inNamespace(namespace)
                .withName(secretName);

        var secret = secretSelector.get();

        if (secret == null) {
            logger.info("Creating Secret " + secretName);

            secretSelector.create(
                    buildSecret(secretName, namespace, fileName, content, ownerReferences));
        } else {
            logger.info("Trying to update Secret " + secretName);
            var updatedData = secret.getData();
            if (updatedData != null) {
                if (updatedData.containsKey(fileName) &&
                        new String(base64Decoder.decode(updatedData.get(fileName)), StandardCharsets.UTF_8).equals(content)) {
                    logger.info("Secret is already updated");
                    return;
                } else {
                    logger.info("Secret content changed, updating " + updatedData.containsKey(fileName));
                }
            } else {
                logger.info("Secret content not available, adding");
                updatedData = new HashMap<>();
            }
            updatedData.put(fileName, base64Encoder.encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            secret.setData(updatedData);
            secretSelector.patch(secret);
        }
    }

}
