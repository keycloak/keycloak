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
import io.fabric8.kubernetes.api.model.MicroTime;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.events.v1.Event;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Loggable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.quarkiverse.operatorsdk.runtime.QuarkusConfigurationService;
import io.quarkus.logging.Log;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.testsuite.utils.K8sUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.operator.Utils.isOpenShift;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;

public abstract class BaseOperatorTest {

  public static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
  public static final String OPERATOR_DEPLOYMENT_PROP = "test.operator.deployment";
  public static final String TARGET_KUBERNETES_GENERATED_YML_FOLDER = "target/kubernetes/";
  public static final String OPERATOR_KUBERNETES_IP = "test.operator.kubernetes.ip";
  public static final String OPERATOR_CUSTOM_IMAGE = "test.operator.custom.image";
  public static final String POSTGRESQL_NAME = "postgresql-db";

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
  protected static boolean isOpenShift;


  @BeforeAll
  public static void before() throws FileNotFoundException {
    configuration = CDI.current().select(QuarkusConfigurationService.class).get();
    reconcilers = CDI.current().select(new TypeLiteral<>() {});
    operatorDeployment = ConfigProvider.getConfig().getOptionalValue(OPERATOR_DEPLOYMENT_PROP, OperatorDeployment.class).orElse(OperatorDeployment.local);
    deploymentTarget = ConfigProvider.getConfig().getOptionalValue(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, String.class).orElse("kubernetes");
    customImage = ConfigProvider.getConfig().getOptionalValue(OPERATOR_CUSTOM_IMAGE, String.class).orElse(null);

    setDefaultAwaitilityTimings();
    calculateNamespace();
    createK8sClient();
    kubernetesIp = ConfigProvider.getConfig().getOptionalValue(OPERATOR_KUBERNETES_IP, String.class).orElseGet(() -> {
        try {
            return new URL(k8sclient.getConfiguration().getMasterUrl()).getHost();
        } catch (MalformedURLException e) {
            return "localhost";
        }
    });
    Log.info("Creating CRDs");
    createCRDs(k8sclient);
    createNamespace();
    isOpenShift = isOpenShift(k8sclient);

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
  public void beforeEach(TestInfo testInfo) {
    String testClassName = testInfo.getTestClass().map(c -> c.getSimpleName() + ".").orElse("");
    Log.info("\n------- STARTING: " + testClassName + testInfo.getDisplayName() + "\n"
            + "------- Namespace: " + namespace + "\n"
            + "------- Mode: " + ((operatorDeployment == OperatorDeployment.remote) ? "remote" : "local"));
  }

  private static void createK8sClient() {
    k8sclient = new KubernetesClientBuilder().withConfig(new ConfigBuilder(Config.autoConfigure(null)).withNamespace(namespace).build()).build();
  }

  private static void createRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Creating RBAC and Deployment into Namespace " + namespace);
    K8sUtils.set(k8sclient, new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + deploymentTarget + ".yml"));
  }

  private static void cleanRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Deleting RBAC from Namespace " + namespace);
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER +deploymentTarget+".yml"))
            .inNamespace(namespace).delete();
  }

  static void createCRDs(KubernetesClient client) throws FileNotFoundException {
    K8sUtils.set(client, new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloaks.k8s.keycloak.org-v1.yml"));
    K8sUtils.set(client, new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloakrealmimports.k8s.keycloak.org-v1.yml"));

    Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> client.resources(Keycloak.class).list());
    Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> client.resources(KeycloakRealmImport.class).list());
  }

  private static void registerReconcilers() {
    Log.info("Registering reconcilers for operator : " + operator + " [" + operatorDeployment + "]");

    for (Reconciler<?> reconciler : reconcilers) {
      Log.info("Register and apply : " + reconciler.getClass().getName());
      operator.register(reconciler);
    }
  }

  private static void createOperator() {
    operator = new Operator(overrider -> overrider.withKubernetesClient(k8sclient));
  }

  private static void createNamespace() {
    Log.info("Creating Namespace " + namespace);
    k8sclient.resource(new NamespaceBuilder().withNewMetadata().addToLabels("app","keycloak-test").withName(namespace).endMetadata().build()).create();
    // ensure that the client defaults to the namespace - eventually most of the test code usage of inNamespace can be removed
    k8sclient = k8sclient.adapt(NamespacedKubernetesClient.class).inNamespace(namespace);
  }

  private static void calculateNamespace() {
    namespace = getNewRandomNamespaceName();
  }

  public static String getNewRandomNamespaceName() {
      return "keycloak-test-" + UUID.randomUUID();
  }

  protected static void deployDB() {
    deployDBSecret();

    // DB
    Log.info("Creating new PostgreSQL deployment");
    K8sUtils.set(k8sclient, BaseOperatorTest.class.getResourceAsStream("/example-postgres.yaml"));

    // Check DB has deployed and ready
    Log.info("Checking Postgres is running");
    Awaitility.await()
            .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().inNamespace(namespace).withName("postgresql-db").get().getStatus().getReadyReplicas()).isEqualTo(1));
  }

  protected static void deployDBSecret() {
    K8sUtils.set(k8sclient, getResourceFromFile("example-db-secret.yaml", Secret.class));
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

  // This differs from later branches as to not introduce the additional test file changes
  protected void savePodLogs() {
    logEvents();
    // provide some helpful entries in the main log as well
    logFailedKeycloaks();
    if (operatorDeployment == OperatorDeployment.remote) {
      log(k8sclient.apps().deployments().withName("keycloak-operator"), Deployment::getStatus, false);
    }
    logFailed(k8sclient.apps().statefulSets().withName(POSTGRESQL_NAME), StatefulSet::getStatus);
    k8sclient.pods().withLabel("app", "keycloak-realm-import").list().getItems().stream()
        .forEach(pod -> log(k8sclient.pods().resource(pod), Pod::getStatus, false));
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
        Log.errorf("Error saving pod logs: %s", e.getMessage());
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

      // due to https://github.com/operator-framework/java-operator-sdk/issues/2314 we
      // try to ensure that the operator has processed the delete event from root objects
      // this can be simplified to just the root deletion after we pick up the fix
      // it can be further simplified after https://github.com/fabric8io/kubernetes-client/issues/5838
      // to just a timed foreground deletion
      var roots = List.of(Keycloak.class, KeycloakRealmImport.class);
      var dependents = List.of(StatefulSet.class, Secret.class, Service.class, Pod.class, Job.class);

      var rootsDeleted = CompletableFuture.allOf(roots.stream()
              .map(c -> k8sclient.resources(c).informOnCondition(List::isEmpty)).toArray(CompletableFuture[]::new));
      roots.stream().forEach(c -> k8sclient.resources(c).withGracePeriod(0).delete());
      try {
          rootsDeleted.get(1, TimeUnit.MINUTES);
      } catch (Exception e) {
          // delete event should have arrived quickly because this is a background delete
          throw new RuntimeException(e);
      }
      dependents.stream().map(c -> k8sclient.resources(c).withLabels(Constants.DEFAULT_LABELS))
              .forEach(r -> r.withGracePeriod(0).delete());
      // enforce that the dependents are gone
      Awaitility.await().during(5, TimeUnit.SECONDS).until(() -> {
          if (dependents.stream().anyMatch(
                  c -> !k8sclient.resources(c).withLabels(Constants.DEFAULT_LABELS).list().getItems().isEmpty())) {
              // the operator must have recreated because it hasn't gotten the keycloak
              // deleted event, keep cleaning
              dependents.stream().map(c -> k8sclient.resources(c).withLabels(Constants.DEFAULT_LABELS))
                      .forEach(r -> r.withGracePeriod(0).delete());
              return false;
          }
          return true;
      });
  }

  private <T extends HasMetadata, R extends Resource<T> & Loggable> void log(R resource, Function<T, Object> statusExtractor, boolean failedOnly) {
      var instance = resource.get();
      if (failedOnly) {
          if (resource.isReady()) {
              return;
          }
          Log.warnf("%s failed to become ready %s", instance.getMetadata().getName(), Serialization.asYaml(statusExtractor.apply(instance)));
      } else {
          Log.infof("%s is ready %s %s", instance.getMetadata().getName(), resource.isReady(), Serialization.asYaml(statusExtractor.apply(instance)));
      }
      try {
          String log = resource.getLog();
          log = log.substring(Math.max(0, log.length() - 50000));
          Log.warnf("%s log: %s", instance.getMetadata().getName(), log);
      } catch (KubernetesClientException e) {
          Log.warnf("No %s log: %s", instance.getMetadata().getName(), e.getMessage());
          if (instance instanceof Pod) {
              try {
                  String previous = k8sclient.raw(String.format("/api/v1/namespaces/%s/pods/%s/log?previous=true", namespace, instance.getMetadata().getName()));
                  Log.warnf("%s previous log: %s", instance.getMetadata().getName(), previous);
              } catch (KubernetesClientException pe) {
                  // not available
                  if (pe.getCode() != HttpURLConnection.HTTP_BAD_REQUEST) {
                      Log.infof("Could not obtain previous log for %s: %s", instance.getMetadata().getName(), e.getMessage());
                  }
              }
          }
      }
  }

  private <T extends HasMetadata, R extends Resource<T> & Loggable> void logFailed(R resource, Function<T, Object> statusExtractor) {
      log(resource, statusExtractor, true);
  }

  private void logFailedKeycloaks() {
      k8sclient.resources(Keycloak.class).list().getItems().stream()
              .filter(kc -> !Optional.ofNullable(kc.getStatus()).map(KeycloakStatus::isReady).orElse(false))
              .forEach(kc -> {
                  Log.warnf("Keycloak failed to become ready \"%s\" %s", kc.getMetadata().getName(), Serialization.asYaml(kc.getStatus()));
                  var statefulSet = k8sclient.apps().statefulSets().withName(kc.getMetadata().getName()).get();
                  if (statefulSet != null) {
                      Log.warnf("Keycloak \"%s\" StatefulSet status %s", kc.getMetadata().getName(), Serialization.asYaml(statefulSet.getStatus()));
                      k8sclient.pods().withLabels(statefulSet.getSpec().getSelector().getMatchLabels()).list()
                              .getItems().stream().map(pod -> k8sclient.pods().resource(pod)).forEach(p -> {
                                  logFailed(p, Pod::getStatus);
                                  threadDump(p);
                              });
                  }
              });
  }

  private void threadDump(PodResource pr) {
      int exitCode = -1;
      Exception ex = null;
      String output = null;
      Pod pod = pr.item();
      try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ExecWatch execWatch = pr.writingOutput(baos).withReadyWaitTimeout(0).exec("sh", "-c", "jcmd 1 Thread.print");
          exitCode = execWatch.exitCode().get(1, TimeUnit.MINUTES);
          output = baos.toString(StandardCharsets.UTF_8);
          if (exitCode == 0) {
              Log.info("Thread dump for " + pod.getMetadata().getName() + ": " + output);
          }
      } catch (Exception e) {
          ex = e;
      }
      if (exitCode != 0) {
          Log.warn("A thread dump was not successful for " + pod.getMetadata().getName()
                  + ", exit code " + exitCode + " output: " + output, ex);
      }
  }

  private void logEvents() {
      List<Event> recentEventList = k8sclient.resources(Event.class).list().getItems();

      var grouped = recentEventList.stream()
              .sorted(Comparator.comparing(BaseOperatorTest::getTime, Comparator.nullsFirst(Comparator.reverseOrder())))
              .collect(
                      Collectors.groupingBy(
                              event -> java.util.Arrays.asList(event.getType(), event.getReason(),
                                      event.getMetadata().getName(), event.getNote()),
                              LinkedHashMap::new, Collectors.toList()))
              .entrySet().iterator();

      for (int i = 0; i < 50 && grouped.hasNext(); i++) {
          var entry = grouped.next();
          Log.logf("Normal".equals(entry.getValue().get(0).getType()) ? Level.INFO : Level.WARN,
                  "Event last seen %s repeated %s times - %s", getTime(entry.getValue().get(0)),
                  entry.getValue().size(), entry.getKey().stream().collect(Collectors.joining(" ")));
      }
  }

  private static String getTime(Event event) {
      return Optional.ofNullable(event.getEventTime()).map(MicroTime::getTime).orElse(event.getDeprecatedLastTimestamp());
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
    assertThat(k8sclient.namespaces().withName(namespace).delete()).isNotNull();
    k8sclient.close();
  }

  public static String getCurrentNamespace() {
    return namespace;
  }
}
