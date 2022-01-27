package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.quarkiverse.operatorsdk.runtime.OperatorProducer;
import io.quarkiverse.operatorsdk.runtime.QuarkusConfigurationService;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ClusterOperatorTest {

  public static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
  public static final String OPERATOR_DEPLOYMENT_PROP = "test.operator.deployment";
  public static final String TARGET_KUBERNETES_GENERATED_YML_FOLDER = "target/kubernetes/";

  public enum OperatorDeployment {local,remote}

  protected static OperatorDeployment operatorDeployment;
  protected static Instance<Reconciler<? extends HasMetadata>> reconcilers;
  protected static QuarkusConfigurationService configuration;
  protected static KubernetesClient k8sclient;
  protected static String namespace;
  protected static String deploymentTarget;
  private static Operator operator;


  @BeforeAll
  public static void before() throws FileNotFoundException {
    configuration = CDI.current().select(QuarkusConfigurationService.class).get();
    reconcilers = CDI.current().select(new TypeLiteral<>() {});
    operatorDeployment = ConfigProvider.getConfig().getOptionalValue(OPERATOR_DEPLOYMENT_PROP, OperatorDeployment.class).orElse(OperatorDeployment.local);
    deploymentTarget = ConfigProvider.getConfig().getOptionalValue(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, String.class).orElse("kubernetes");

    calculateNamespace();
    createK8sClient();
    createNamespace();

    if (operatorDeployment == OperatorDeployment.remote) {
      createCRDs();
      createRBACresourcesAndOperatorDeployment();
    } else {
      createOperator();
      registerReconcilers();
      operator.start();
    }

  }

  private static void createK8sClient() {
    k8sclient = new DefaultKubernetesClient(new ConfigBuilder(Config.autoConfigure(null)).withNamespace(namespace).build());
  }

  private static void createRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Creating RBAC into Namespace " + namespace);
    List<HasMetadata> hasMetadata = k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + deploymentTarget + ".yml"))
            .inNamespace(namespace).get();
    hasMetadata.stream()
            .map(b -> {
              if ("Deployment".equalsIgnoreCase(b.getKind()) && b.getMetadata().getName().contains("operator")) {
                ((Deployment) b).getSpec().getTemplate().getSpec().getContainers().get(0).setImagePullPolicy("Never");
              }
              return b;
            }).forEach(c -> {
              Log.info("processing part : " + c.getKind() + "--" + c.getMetadata().getName() + " -- " + namespace);
              k8sclient.resource(c).inNamespace(namespace).createOrReplace();
            });
  }

  private static void cleanRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Deleting RBAC from Namespace " + namespace);
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER +deploymentTarget+".yml"))
            .inNamespace(namespace).delete();
  }
  private static void createCRDs() throws FileNotFoundException {
    Log.info("Creating CRDs");
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloaks.keycloak.org-v1.yml")).createOrReplace();
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloakrealmimports.keycloak.org-v1.yml")).createOrReplace();
  }

  private static void registerReconcilers() {
    Log.info("Registering reconcilers for operator : " + operator + " [" + operatorDeployment + "]");

    for (Reconciler reconciler : reconcilers) {
      final var config = configuration.getConfigurationFor(reconciler);
      if (!config.isRegistrationDelayed()) {
        Log.info("Register and apply : " + reconciler.getClass().getName());
        OperatorProducer.applyCRDIfNeededAndRegister(operator, reconciler, configuration);
      }
    }
  }

  private static void createOperator() {
    operator = new Operator(k8sclient, configuration);
    operator.getConfigurationService().getClientConfiguration().setNamespace(namespace);
  }

  private static void createNamespace() {
    Log.info("Creating Namespace " + namespace);
    k8sclient.namespaces().create(new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build());
  }

  private static void calculateNamespace() {
    namespace = "keycloak-test-" + UUID.randomUUID();
  }

  @AfterAll
  public static void after() throws FileNotFoundException {

    if (operatorDeployment == OperatorDeployment.local) {
      Log.info("Stopping Operator");
      operator.stop();

      Log.info("Creating new K8s Client");
      // create a new client bc operator has closed the old one
      createK8sClient();
    } else {
      cleanRBACresourcesAndOperatorDeployment();
    }

    Log.info("Deleting namespace : " + namespace);
    assertThat(k8sclient.namespaces().withName(namespace).delete()).isTrue();
    k8sclient.close();
  }
}
