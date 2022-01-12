package org.keycloak.operator.v2alpha1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.realm.Realm;

import java.util.HashMap;
import java.util.List;

import static org.keycloak.operator.Constants.MANAGED_BY_LABEL;
import static org.keycloak.operator.Constants.MANAGED_BY_VALUE;
import static org.keycloak.operator.v2alpha1.NameUtils.getValidDNSSubdomainName;

public class RealmSecret {

    private final static Logger logger = Logger.getLogger(RealmSecret.class);

    private KubernetesClient client;
    private ObjectMapper jsonMapper;

    public RealmSecret(KubernetesClient client, ObjectMapper jsonMapper) {
        this.client = client;
        this.jsonMapper = jsonMapper;
    }

    public static String getSecretName(Deployment deployment, Realm realm) {
        return getValidDNSSubdomainName(deployment.getMetadata().getName() + "-" + realm.getSpec().getRealm().getRealm() + "-realm");
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
            var updatedData = secret.getStringData();
            if (updatedData != null) {
                if (updatedData.containsKey(fileName) && updatedData.get(fileName).equals(content)) {
                    logger.info("Secret was already updated");
                    return;
                } else {
                    logger.info("Secret content changed, updating");
                }
            } else {
                logger.info("Secret content not available, adding");
                updatedData = new HashMap<String, String>();
            }
            updatedData.put(fileName, content);
            secret.setStringData(updatedData);
            secretSelector.patch(secret);
        }
    }

}
