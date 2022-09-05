/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.testsuite.integration;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.quarkiverse.operatorsdk.runtime.OperatorProducer;
import io.quarkiverse.operatorsdk.runtime.QuarkusConfigurationService;
import io.quarkus.logging.Log;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;

public abstract class BaseOperatorTest {

  public static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
  public static final String OPERATOR_DEPLOYMENT_PROP = "test.operator.deployment";
  public static final String TARGET_KUBERNETES_GENERATED_YML_FOLDER = "target/kubernetes/";
  public static final String OPERATOR_KUBERNETES_IP = "test.operator.kubernetes.ip";
  public static final String OPERATOR_CUSTOM_IMAGE = "test.operator.custom.image";

  public static final String TEST_RESULTS_DIR = "target/operator-test-results/";
  public static final String POD_LOGS_DIR = TEST_RESULTS_DIR + "pod-logs/";

  public enum OperatorDeployment {local,remote}

  protected static OperatorDeployment operatorDeployment;
  protected static Instance<Reconciler<? extends HasMetadata>> reconcilers;
  protected static QuarkusConfigurationService configuration;
  protected static KubernetesClient k8sclient;
  protected static String namespace;
  protected static String deploymentTarget;
  protected static String kubernetesIp;
  protected static String customImage;
  private static Operator operator;


  @BeforeAll
  public static void before() throws FileNotFoundException {
    configuration = CDI.current().select(QuarkusConfigurationService.class).get();
    reconcilers = CDI.current().select(new TypeLiteral<>() {});
    operatorDeployment = ConfigProvider.getConfig().getOptionalValue(OPERATOR_DEPLOYMENT_PROP, OperatorDeployment.class).orElse(OperatorDeployment.local);
    deploymentTarget = ConfigProvider.getConfig().getOptionalValue(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, String.class).orElse("kubernetes");
    kubernetesIp = ConfigProvider.getConfig().getOptionalValue(OPERATOR_KUBERNETES_IP, String.class).orElse("localhost");
    customImage = ConfigProvider.getConfig().getOptionalValue(OPERATOR_CUSTOM_IMAGE, String.class).orElse(null);

    setDefaultAwaitilityTimings();
    calculateNamespace();
    createK8sClient();
    createCRDs();
    createNamespace();

    if (operatorDeployment == OperatorDeployment.remote) {
      createRBACresourcesAndOperatorDeployment();
    } else {
      createOperator();
      registerReconcilers();
      operator.start();
    }

    deployDB();
  }

  @BeforeEach
  public void beforeEach() {
    Log.info(((operatorDeployment == OperatorDeployment.remote) ? "Remote " : "Local ") + "Run Test :" + namespace);
  }

  private static void createK8sClient() {
    k8sclient = new DefaultKubernetesClient(new ConfigBuilder(Config.autoConfigure(null)).withNamespace(namespace).build());
  }

  private static void createRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Creating RBAC and Deployment into Namespace " + namespace);
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + deploymentTarget + ".yml"))
            .inNamespace(namespace).createOrReplace();
  }

  private static void cleanRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Deleting RBAC from Namespace " + namespace);
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER +deploymentTarget+".yml"))
            .inNamespace(namespace).delete();
  }

  private static void createCRDs() {
    Log.info("Creating CRDs");
    try {
      var deploymentCRD = k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloaks.k8s.keycloak.org-v1.yml"));
      deploymentCRD.createOrReplace();
      deploymentCRD.waitUntilReady(5, TimeUnit.SECONDS);
      var realmImportCRD = k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloakrealmimports.k8s.keycloak.org-v1.yml"));
      realmImportCRD.createOrReplace();
      realmImportCRD.waitUntilReady(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      Log.warn("Failed to create Keycloak CRD, retrying", e);
      createCRDs();
    }
  }

  private static void registerReconcilers() {
    Log.info("Registering reconcilers for operator : " + operator + " [" + operatorDeployment + "]");

    for (Reconciler<?> reconciler : reconcilers) {
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

  protected static void deployDB() {
    // DB
    Log.info("Creating new PostgreSQL deployment");
    k8sclient.load(BaseOperatorTest.class.getResourceAsStream("/example-postgres.yaml")).inNamespace(namespace).createOrReplace();

    // Check DB has deployed and ready
    Log.info("Checking Postgres is running");
    Awaitility.await()
            .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName("postgresql-db").get().getStatus().getReadyReplicas()).isEqualTo(1));

    deployDBSecret();
  }

  protected static void deployDBSecret() {
    k8sclient.secrets().inNamespace(namespace).createOrReplace(getResourceFromFile("example-db-secret.yaml", Secret.class));
  }

  protected static void deleteDB() {
    // Delete the Postgres StatefulSet
    k8sclient.apps().statefulSets().inNamespace(namespace).withName("postgresql-db").delete();
    Awaitility.await()
            .ignoreExceptions()
            .untilAsserted(() -> {
              Log.infof("Waiting for postgres to be deleted");
              assertThat(k8sclient
                      .apps()
                      .statefulSets()
                      .inNamespace(namespace)
                      .withName("postgresql-db")
                      .get()).isNull();
            });
  }

  // TODO improve this (preferably move to JOSDK)
  protected void savePodLogs() {
    Log.infof("Saving pod logs to %s", POD_LOGS_DIR);
    for (var pod : k8sclient.pods().inNamespace(namespace).list().getItems()) {
      try {
        String podName = pod.getMetadata().getName();
        Log.infof("Processing %s", podName);
        String podLog = k8sclient.pods().inNamespace(namespace).withName(podName).getLog();
        File file = new File(POD_LOGS_DIR + String.format("%s-%s.txt", namespace, podName)); // using namespace for now, if more tests fail, the log might get overwritten
        file.getAbsoluteFile().getParentFile().mkdirs();
        try (var fw = new FileWriter(file, false)) {
          fw.write(podLog);
        }
      } catch (Exception e) {
        Log.error(e.getStackTrace());
      }
    }
  }

  private static void setDefaultAwaitilityTimings() {
    Awaitility.setDefaultPollInterval(Duration.ofSeconds(1));
    Awaitility.setDefaultTimeout(Duration.ofSeconds(360));
  }

  @AfterEach
  public void cleanup() {
    Log.info("Deleting Keycloak CR");
    k8sclient.resources(Keycloak.class).delete();
    Awaitility.await()
            .untilAsserted(() -> {
              var kcDeployments = k8sclient
                      .apps()
                      .statefulSets()
                      .inNamespace(namespace)
                      .withLabels(Constants.DEFAULT_LABELS)
                      .list()
                      .getItems();
              assertThat(kcDeployments.size()).isZero();
            });
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
