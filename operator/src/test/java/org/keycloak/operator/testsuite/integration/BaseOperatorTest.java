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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.MicroTime;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.events.v1.Event;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Loggable;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.config.BaseConfigurationService;
import io.javaoperatorsdk.operator.api.config.InformerStoppedHandler;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.quarkiverse.operatorsdk.runtime.QuarkusConfigurationService;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakController;
import org.keycloak.operator.controllers.KeycloakDeploymentDependentResource;
import org.keycloak.operator.controllers.KeycloakRealmImportController;
import org.keycloak.operator.controllers.KeycloakUpdateJobDependentResource;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpecBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatus;
import org.keycloak.operator.testsuite.apiserver.ApiServerHelper;
import org.keycloak.operator.testsuite.apiserver.DisabledIfApiServerTest;
import org.keycloak.operator.testsuite.utils.K8sUtils;
import org.opentest4j.TestAbortedException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.lang.reflect.Method;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.keycloak.operator.Utils.isOpenShift;
import static org.keycloak.operator.testsuite.utils.K8sUtils.getResourceFromFile;

public class BaseOperatorTest implements QuarkusTestAfterEachCallback {

  public static final String SLOW = "slow";

  public static final String QUARKUS_KUBERNETES_DEPLOYMENT_TARGET = "quarkus.kubernetes.deployment-target";
  public static final String OPERATOR_DEPLOYMENT_PROP = "test.operator.deployment";
  public static final String TARGET_KUBERNETES_GENERATED_YML_FOLDER = "target/kubernetes/";
  public static final String OPERATOR_KUBERNETES_IP = "test.operator.kubernetes.ip";
  public static final String OPERATOR_CUSTOM_IMAGE = "test.operator.custom.image";
  public static final String POSTGRESQL_NAME = "postgresql-db";

  public static final String TEST_RESULTS_DIR = "target/operator-test-results/";
  public static final String POD_LOGS_DIR = TEST_RESULTS_DIR + "pod-logs/";

  private static final class BaseOperatorTestConfigurationService extends BaseConfigurationService {

    private final KubernetesClient client;
    private boolean stopped;

    private BaseOperatorTestConfigurationService(KubernetesClient client) {
        this.client = client;
    }

    @Override
    public KubernetesClient getKubernetesClient() {
        return client;
    }

    @Override
    public Optional<InformerStoppedHandler> getInformerStoppedHandler() {
        return Optional.of((informer, ex) -> {
            if (ex != null && informer.hasSynced()) {
                if (!stopped) {
                    log.error("Fatal error in informer: {}.", informer, ex);
                    stopped = true;
                }
            } else {
                super.getInformerStoppedHandler().ifPresent(handler -> handler.onStop(informer, ex));
            }
        });
    }
}

public enum OperatorDeployment {local_apiserver,local,remote}

  protected static OperatorDeployment operatorDeployment;
  protected static QuarkusConfigurationService configuration;
  protected static KubernetesClient k8sclient;
  protected static String namespace;
  protected static String deploymentTarget;
  protected static String kubernetesIp;
  protected static String customImage;
  private static Operator operator;
  private static BaseOperatorTestConfigurationService config;
  protected static boolean isOpenShift;

  private static ApiServerHelper kubeApi;

  @BeforeAll
  public static void before(TestInfo testInfo) throws FileNotFoundException {
    configuration = CDI.current().select(QuarkusConfigurationService.class).get();
    operatorDeployment = ConfigProvider.getConfig().getOptionalValue(OPERATOR_DEPLOYMENT_PROP, OperatorDeployment.class).orElse(OperatorDeployment.local_apiserver);
    if (testInfo.getTestClass().map(m -> m.getAnnotation(DisabledIfApiServerTest.class)).isPresent()) {
      Assumptions.assumeFalse(operatorDeployment == OperatorDeployment.local_apiserver);
    }
    deploymentTarget = ConfigProvider.getConfig().getOptionalValue(QUARKUS_KUBERNETES_DEPLOYMENT_TARGET, String.class).orElse("kubernetes");
    customImage = ConfigProvider.getConfig().getOptionalValue(OPERATOR_CUSTOM_IMAGE, String.class).orElse(null);

    setDefaultAwaitilityTimings();

    if (operatorDeployment == OperatorDeployment.local_apiserver && kubeApi == null) {
      kubeApi = new ApiServerHelper();
    }

    createK8sClientForRandomNamespace();
    kubernetesIp = ConfigProvider.getConfig().getOptionalValue(OPERATOR_KUBERNETES_IP, String.class).orElseGet(() -> {
        try {
            return new URL(k8sclient.getConfiguration().getMasterUrl()).getHost();
        } catch (MalformedURLException e) {
            return "localhost";
        }
    });
    Log.info("Creating CRDs");
    createCRDs(k8sclient);
    isOpenShift = isOpenShift(k8sclient);

    if (operatorDeployment == OperatorDeployment.remote) {
      createRBACresourcesAndOperatorDeployment();
    } else {
      createOperator();
    }

    if (operatorDeployment == OperatorDeployment.local_apiserver) {
      deployDBSecret();
    } else {
      deployDB();
    }
  }

  @BeforeEach
  public void beforeEach(TestInfo testInfo) {
      if (testInfo.getTestMethod().map(m -> m.getAnnotation(DisabledIfApiServerTest.class)).isPresent()) {
          Assumptions.assumeTrue(operatorDeployment != OperatorDeployment.local_apiserver);
      }
      String testClassName = testInfo.getTestClass().map(c -> c.getSimpleName() + ".").orElse("");
      Log.info("\n------- STARTING: " + testClassName + testInfo.getDisplayName() + "\n"
              + "------- Namespace: " + namespace + "\n"
              + "------- Mode: " + operatorDeployment.name());
  }

  private static void createK8sClientForRandomNamespace() {
      namespace = getNewRandomNamespaceName();
      Log.infof("Creating new K8s Client for namespace %s", namespace);
      if (operatorDeployment == OperatorDeployment.local_apiserver) {
          k8sclient = kubeApi.createClient(namespace);
      } else {
          k8sclient = new KubernetesClientBuilder().withConfig(new ConfigBuilder(Config.autoConfigure(null)).withNamespace(namespace).build()).build();
      }
      Log.info("Creating Namespace " + namespace);
      k8sclient.resource(new NamespaceBuilder().withNewMetadata().addToLabels("app","keycloak-test").withName(namespace).endMetadata().build()).create();
  }

  private static void createRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Creating RBAC and Deployment into Namespace " + namespace);
    K8sUtils.set(k8sclient, new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + deploymentTarget + ".yml"), obj -> {
        if (obj instanceof ClusterRoleBinding) {
            ((ClusterRoleBinding)obj).getSubjects().forEach(s -> s.setNamespace(namespace));
        } else if (obj instanceof RoleBinding && "keycloak-operator-view".equals(obj.getMetadata().getName())) {
            return null; // exclude this role since it's not present in olm
        } else if (obj instanceof Deployment) {
            // set values useful for testing - TODO: could drive this in some way from the test/resource/application.properties
            ((Deployment)obj).getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().add(new EnvVar("KC_OPERATOR_KEYCLOAK_UPDATE_POD_DEADLINE_SECONDS", "60", null));
        }
        return obj;
    });
  }

  private static void cleanRBACresourcesAndOperatorDeployment() throws FileNotFoundException {
    Log.info("Deleting RBAC from Namespace " + namespace);
    k8sclient.load(new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER +deploymentTarget+".yml"))
            .inNamespace(namespace).delete();
  }

  public static void createCRDs(KubernetesClient client) throws FileNotFoundException {
    K8sUtils.set(client, new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloaks.k8s.keycloak.org-v1.yml"));
    K8sUtils.set(client, new FileInputStream(TARGET_KUBERNETES_GENERATED_YML_FOLDER + "keycloakrealmimports.k8s.keycloak.org-v1.yml"));
    K8sUtils.set(client, BaseOperatorTest.class.getResourceAsStream("/service-monitor-crds.yml"));

    Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> client.resources(Keycloak.class).list());
    Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> client.resources(KeycloakRealmImport.class).list());
    Awaitility.await().pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> client.resources(ServiceMonitor.class).list());
  }

  private static void createOperator() {
    // create the operator to use the current client / namespace and injected dependent resources
    // to be replaced later with full cdi construction or test mechanics from quarkus operator sdk
    config = new BaseOperatorTestConfigurationService(k8sclient);
    operator = new Operator(config);
    Log.info("Registering reconcilers for operator : " + operator + " [" + operatorDeployment + "]");

    Instance<Reconciler<? extends HasMetadata>> reconcilers = CDI.current().select(new TypeLiteral<>() {});

    for (Reconciler<?> reconciler : reconcilers) {
      Log.info("Register and apply : " + reconciler.getClass().getName());
      operator.register(reconciler, overrider -> overrider.settingNamespace(k8sclient.getNamespace()));
    }
    operator.start();
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
            .untilAsserted(() -> assertThat(k8sclient.apps().statefulSets().withName(POSTGRESQL_NAME).get().getStatus().getReadyReplicas()).isEqualTo(1));
  }

  protected static void deployDBSecret() {
    K8sUtils.set(k8sclient, getResourceFromFile("example-db-secret.yaml", Secret.class));
  }

  protected static void deleteDB() {
    // Delete the Postgres StatefulSet
    Log.infof("Waiting for postgres to be deleted");
    k8sclient.apps().statefulSets().withName(POSTGRESQL_NAME).withTimeout(2, TimeUnit.MINUTES).delete();
  }

  // TODO improve this (preferably move to JOSDK)
  protected void savePodLogs() {
    Log.infof("Saving pod logs to %s", POD_LOGS_DIR);
    for (var pod : k8sclient.pods().list().getItems()) {
      try {
        String podName = pod.getMetadata().getName();
        Log.infof("Processing %s", podName);
        String podLog = k8sclient.pods().withName(podName).getLog();
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
    if (operatorDeployment == OperatorDeployment.local_apiserver) {
        Awaitility.setDefaultPollInterval(Duration.ofMillis(250));
        Awaitility.setDefaultTimeout(Duration.ofSeconds(60));
    } else {
        Awaitility.setDefaultPollInterval(Duration.ofMillis(500));
        Awaitility.setDefaultTimeout(Duration.ofSeconds(360));
    }
  }

  public void cleanup() {
      if (operatorDeployment == OperatorDeployment.local_apiserver) {
          // by default garbage collection is not supported by envtest
          // so might as well do a namespace per test
          String oldNamespace = namespace;
          stopOperator();
          // create a new client bc operator has closed the old one
          createK8sClientForRandomNamespace();
          k8sclient.namespaces().withName(oldNamespace).delete();
          createOperator();
          deployDBSecret();
          return;
      }
      Log.info("Deleting Keycloak CR");

      // first graceful scaledown
      k8sclient.resources(Keycloak.class).list().getItems().forEach(
              k -> k8sclient.resource(new KeycloakBuilder(k).editSpec().withInstances(0).endSpec().build()).unlock().patch());

      try {
          k8sclient.resources(Keycloak.class).informOnCondition(
                  l -> l.stream().allMatch(k -> Optional.ofNullable(k.getStatus()).map(KeycloakStatus::getInstances).orElse(0).equals(0)))
                  .get(40, TimeUnit.SECONDS);
      } catch (Exception e) {
          throw KubernetesClientException.launderThrowable(e);
      }

      // this can be simplified to just the root deletion after we pick up the fix
      // it can be further simplified after https://github.com/fabric8io/kubernetes-client/issues/5838
      // to just a timed foreground deletion
      var roots = List.of(Keycloak.class, KeycloakRealmImport.class);
      roots.forEach(c -> k8sclient.resources(c).delete());
      // enforce that at least the statefulset are gone
      try {
          k8sclient
                  .apps()
                  .statefulSets()
                  .withLabels(Constants.DEFAULT_LABELS).informOnCondition(List::isEmpty).get(20, TimeUnit.SECONDS);
      } catch (Exception e) {
          throw KubernetesClientException.launderThrowable(e);
      }
  }

  @Override
  public void afterEach(QuarkusTestMethodContext context) {
      if (!(context.getTestInstance() instanceof BaseOperatorTest)) {
          return; // this hook gets called for all quarkus tests, not all are operator tests
      }
      try {
          Method testMethod = context.getTestMethod();
          if (context.getTestStatus().getTestErrorCause() == null
                  || context.getTestStatus().getTestErrorCause() instanceof TestAbortedException
                  || Stream.of(context.getTestStatus().getTestErrorCause().getStackTrace())
                          .noneMatch(ste -> ste.getMethodName().equals(testMethod.getName())
                                  && ste.getClassName().equals(testMethod.getDeclaringClass().getName()))) {
              return;
          }
          Log.warnf("Test failed with %s: %s", context.getTestStatus().getTestErrorCause().getMessage(), context.getTestStatus().getTestErrorCause().getClass().getName());
          Log.infof("Secrets %s", k8sclient.secrets().list().getItems().stream().map(s -> s.getMetadata().getName()).collect(Collectors.joining(", ")));
          logEvents();
          savePodLogs();
          // provide some helpful entries in the main log as well
          logKeycloaks();
          logKeycloakRealmImports();
          if (operatorDeployment == OperatorDeployment.remote) {
              log(k8sclient.apps().deployments().withName("keycloak-operator"), Deployment::getStatus, false);
          }
          if (operatorDeployment != OperatorDeployment.local_apiserver) {
              logFailed(k8sclient.apps().statefulSets().withName(POSTGRESQL_NAME), StatefulSet::getStatus);
          }
          k8sclient.pods().withLabel("app", "keycloak-realm-import").list().getItems()
                  .forEach(pod -> log(k8sclient.pods().resource(pod), Pod::getStatus, false));
      } finally {
          cleanup();
      }
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

  private void logKeycloakRealmImports() {
      k8sclient.resources(KeycloakRealmImport.class).list().getItems().stream()
              .forEach(kcri -> {
                  if (Optional.ofNullable(kcri.getStatus()).map(KeycloakRealmImportStatus::isDone).orElse(false)) {
                      Log.infof("Keycloak realm import '%s' status: %s", kcri.getMetadata().getName(), Serialization.asYaml(kcri.getStatus()));
                  } else {
                      Log.warnf("Keycloak realm import failed to be done \"%s\": %s", kcri.getMetadata().getName(), Serialization.asYaml(kcri.getStatus()));

                      Keycloak kc = k8sclient.resources(Keycloak.class).withName(kcri.getSpec().getKeycloakCRName()).get();

                      var job = k8sclient.batch().v1().jobs()
                              .inNamespace(kcri.getMetadata().getNamespace())
                              .withName(KeycloakUpdateJobDependentResource.jobName(kc))
                              .get();
                      if (job != null) {
                          Log.warnf("Keycloak Update Job \"%s\" %s", job.getMetadata().getName(), Serialization.asYaml(job.getStatus()));
                      }
                  }
              });
  }

  private void logKeycloaks() {
      k8sclient.resources(Keycloak.class).list().getItems().stream()
              .forEach(kc -> {
                  if (Optional.ofNullable(kc.getStatus()).map(KeycloakStatus::isReady).orElse(false)) {
                      Log.infof("Keycloak '%s' status: %s", kc.getMetadata().getName(), Serialization.asYaml(kc.getStatus()));
                  } else {
                      Log.warnf("Keycloak failed to become ready \"%s\": %s", kc.getMetadata().getName(), Serialization.asYaml(kc.getStatus()));
                      var statefulSet = k8sclient.apps().statefulSets().withName(KeycloakDeploymentDependentResource.getName(kc)).get();
                      if (statefulSet != null) {
                          Log.warnf("Keycloak \"%s\" StatefulSet status %s", kc.getMetadata().getName(), Serialization.asYaml(statefulSet.getStatus()));
                          k8sclient.pods().withLabels(statefulSet.getSpec().getSelector().getMatchLabels()).list()
                                  .getItems().stream().map(pod -> k8sclient.pods().resource(pod)).forEach(p -> {
                                      logFailed(p, Pod::getStatus);
                                      threadDump(p);
                                  });
                      }
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
                  entry.getValue().size(), String.join(" ", entry.getKey()));
      }
  }

  private static String getTime(Event event) {
      return Optional.ofNullable(event.getEventTime()).map(MicroTime::getTime).orElse(event.getDeprecatedLastTimestamp());
  }

  @AfterAll
  public static void after() throws FileNotFoundException {
      if (operatorDeployment == OperatorDeployment.remote) {
          cleanRBACresourcesAndOperatorDeployment();
      }

      if (k8sclient != null) {
          Log.info("Deleting namespace : " + namespace);
          assertThat(k8sclient.namespaces().withName(namespace).delete()).isNotEmpty();
      }

      if (operator != null) {
          stopOperator();
          operator = null;
      }

      if (k8sclient != null) {
          k8sclient.close();
          k8sclient = null;
      }

      // scope the api server to the entire test run
      /*if (kubeApi != null) {
          kubeApi.stop();
          kubeApi = null;
      }*/
  }

  private static void stopOperator() {
      Log.info("Stopping Operator");
      assertFalse(config.stopped, "An informer unexpected stopped, check log for an ERROR");
      config.stopped = true;
      operator.stop();

      // Avoid issues with Event Informers between tests
      Log.info("Removing Controllers and application scoped DRs from CDI");
      Stream.of(KeycloakController.class, KeycloakRealmImportController.class, KeycloakUpdateJobDependentResource.class)
                      .forEach(c -> CDI.current().destroy(CDI.current().select(c).get()));
  }

  public static String getCurrentNamespace() {
    return namespace;
  }

  public static String getTestCustomImage() {
    return customImage;
  }

  /**
   * Get the default deployment modified/optimized by operator test settings
   * @param disableProbes when true the unsupported template will be used to effectively
   *   disable the probes, which will speed up testing for scenarios that don't interact
   *   with the underlying keycloak
   * @return
   */
  public static Keycloak getTestKeycloakDeployment(boolean disableProbes) {
      return getTestKeycloakDeployment(disableProbes, true);
  }

    public static Keycloak getTestKeycloakDeployment(boolean disableProbes, boolean setCustomImage) {
        Keycloak kc = K8sUtils.getDefaultKeycloakDeployment();
        kc.getMetadata().setNamespace(getCurrentNamespace());
        String image = getTestCustomImage();
        if (setCustomImage && image != null) {
            kc.getSpec().setImage(image);
        }
        if (disableProbes) {
            return disableProbes(kc);
        }
        return kc;
    }

  public static Keycloak disableProbes(Keycloak keycloak) {
      KeycloakSpecBuilder specBuilder = new KeycloakSpecBuilder(keycloak.getSpec());
      var podTemplateSpecBuilder = specBuilder.editOrNewUnsupported().editOrNewPodTemplate().editOrNewSpec();
      var containerBuilder = podTemplateSpecBuilder.hasContainers() ? podTemplateSpecBuilder.editContainer(0)
              : podTemplateSpecBuilder.addNewContainer();
      keycloak.setSpec(containerBuilder.withNewLivenessProbe().withNewExec().addToCommand("true").endExec()
              .endLivenessProbe().withNewReadinessProbe().withNewExec().addToCommand("true").endExec()
              .endReadinessProbe().withNewStartupProbe().withNewExec().addToCommand("true").endExec()
              .endStartupProbe().endContainer().endSpec().endPodTemplate().endUnsupported().build());
      return keycloak;
  }

    protected static String namespaceOf(Keycloak keycloak) {
        return keycloak.getMetadata().getNamespace();
    }

}
