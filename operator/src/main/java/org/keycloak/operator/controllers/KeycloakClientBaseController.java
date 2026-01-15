/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator.controllers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.api.AdminApi;
import org.keycloak.admin.api.client.ClientApi;
import org.keycloak.admin.client.ClientBuilderWrapper;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientSpec;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatus;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.client.KeycloakClientStatusCondition;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.logging.Log;

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

public abstract class KeycloakClientBaseController<R extends CustomResource<? extends KeycloakClientSpec<S>, KeycloakClientStatus>, T extends BaseClientRepresentation, S extends BaseClientRepresentation>
        implements Reconciler<R>, Cleaner<R> {

    private static final String CLIENT_API_VERSION = "v2";
    private static final String HTTPS = "https";
    // TODO allows for local testing - could be promoted in some way to the CR if needed
    public static final String KEYCLOAK_TEST_ADDRESS = "KEYCLOAK_TEST_ADDRESS";
    private DefaultHostnameVerifier verifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());

    class KeycloakClientStatusAggregator {
        R resource;
        KeycloakClientStatus existingStatus;
        Map<String, KeycloakClientStatusCondition> existingConditions;
        Map<String, KeycloakClientStatusCondition> newConditions = new LinkedHashMap<String, KeycloakClientStatusCondition>();

        KeycloakClientStatusAggregator(R resource) {
            this.resource = resource;
            existingStatus = Optional.ofNullable(resource.getStatus()).orElse(new KeycloakClientStatus());
            existingConditions = KeycloakStatusAggregator.getConditionMap(existingStatus.getConditions());
        }

        void setCondition(String type, Boolean status, String message) {
            KeycloakClientStatusCondition condition = new KeycloakClientStatusCondition();
            condition.setStatus(status);
            condition.setMessage(message);
            newConditions.put(type, condition); // No aggregation yet
        }

        KeycloakClientStatus build() {
            KeycloakClientStatusBuilder statusBuilder = new KeycloakClientStatusBuilder();
            String now = Utils.iso8601Now();
            statusBuilder.withObservedGeneration(resource.getMetadata().getGeneration());
            newConditions.values().forEach(c -> KeycloakStatusAggregator.updateConditionFromExisting(c, existingConditions, now));
            existingConditions.putAll(newConditions);
            statusBuilder.withConditions(new ArrayList<>(existingConditions.values().stream().sorted(Comparator.comparing(KeycloakClientStatusCondition::getType)).toList()));
            return statusBuilder.build();
        }

        public KeycloakClientStatus getExistingStatus() {
            return existingStatus;
        }

    }

    @Inject
    Config config;

    @Override
    public UpdateControl<R> reconcile(R resource, Context<R> context) throws Exception {
        String kcName = resource.getSpec().getKeycloakCRName();

        // TODO: this should be obtained from an informer instead
        // they can't be shared directly across controllers, so we'd have to inject the
        // KeycloakController and access via a reference to a saved context
        Keycloak keycloak = context.getClient().resources(Keycloak.class)
                .inNamespace(resource.getMetadata().getNamespace()).withName(kcName).require();

        KeycloakClientStatusAggregator statusAggregator = new KeycloakClientStatusAggregator(resource);

        S client = resource.getSpec().getClient();
        // first convert to the target representation - the spec representation is specialized
        var map = context.getClient().getKubernetesSerialization().convertValue(client, Map.class);
        map.put(BaseClientRepresentation.DISCRIMINATOR_FIELD, client.getProtocol());
        T rep = context.getClient().getKubernetesSerialization().convertValue(map, getTargetRepresentation());
        // then let the controller subclass apply specific handling
        boolean poll = prepareRepresentation(client, rep, context);
        rep.setClientId(resource.getMetadata().getName());

        String hash = Utils.hash(List.of(rep));

        if (!hash.equals(statusAggregator.getExistingStatus().getHash())) {
            var response = invoke(resource, context, keycloak, clientApi -> {
                return clientApi.createOrUpdateClient(rep);
            });

            // if not ok response, throw exception to allow the retry loop
            // TODO however not all errors (something not validating) should get retried every 10 seconds
            // that should instead get captured in the status
            if (response.getStatus() != HttpURLConnection.HTTP_OK && response.getStatus() != HttpURLConnection.HTTP_CREATED) {
                throw new RuntimeException("Client update operation not sucessful with status code " + response.getStatus());
            }
        }

        KeycloakClientStatus status = statusAggregator.build();
        status.setHash(hash);
        statusAggregator.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, false, null);
        UpdateControl<R> updateControl;

        if (status.equals(resource.getStatus())) {
            updateControl = UpdateControl.noUpdate();
        } else {
            resource.setStatus(status);
            updateControl = UpdateControl.patchStatus(resource);
        }

        if (poll) {
            updateControl.rescheduleAfter(config.keycloak().pollIntervalSeconds(), TimeUnit.SECONDS);
        }

        return updateControl;
    }

    abstract boolean prepareRepresentation(S crRepresentation, T targetRepresentation, Context<?> context);

    abstract Class<T> getTargetRepresentation();

    /**
     * Uses a finalizer to ensure clients are not orphaned unless a user goes out of
     * their way to do so
     */
    @Override
    public DeleteControl cleanup(R resource, Context<R> context) throws Exception {
        String kcName = resource.getSpec().getKeycloakCRName();

        Keycloak keycloak = context.getClient().resources(Keycloak.class)
                .inNamespace(resource.getMetadata().getNamespace()).withName(kcName).get();

        if (keycloak == null) {
            return DeleteControl.defaultDelete();
        }

        invoke(resource, context, keycloak, client -> {
            try {
                client.deleteClient();
            } catch (WebApplicationException e) {
                if (e.getResponse().getStatus() != 404) {
                    throw e;
                }
            }
            return null;
        });

        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<R> updateErrorStatus(R resource, Context<R> context, Exception e) {
        Log.error("--- Error reconciling", e);

        KeycloakClientStatusAggregator status = new KeycloakClientStatusAggregator(resource);
        status.setCondition(KeycloakClientStatusCondition.HAS_ERRORS, true, "Error performing operations:\n" + e.getMessage());
        resource.setStatus(status.build());

        return ErrorStatusUpdateControl.patchStatus(resource).rescheduleAfter(Constants.RETRY_DURATION);
    }

    @Path("admin/api")
    public interface AdminRootV2 {

        @Path("{realmName}")
        AdminApi adminApi(@PathParam("realmName") String realmName);

    }

    private <T> T invoke(R resource, Context<?> context, Keycloak keycloak,
            Function<ClientApi, T> action) {
        try (var kcAdmin = getAdminClient(resource, context, keycloak)) {
            var target = getWebTarget(kcAdmin);
            AdminRootV2 root = org.keycloak.admin.client.Keycloak.getClientProvider().targetProxy(target,
                    AdminRootV2.class);
            return action.apply(root.adminApi(resource.getSpec().getRealm()).clients(CLIENT_API_VERSION)
                    .client(resource.getMetadata().getName()));
        }
    }

    private WebTarget getWebTarget(org.keycloak.admin.client.Keycloak kcAdmin) {
        // TODO: change the api
        try {
            Field field = kcAdmin.getClass().getDeclaredField("target");
            field.setAccessible(true);
            return (WebTarget)field.get(kcAdmin);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private org.keycloak.admin.client.Keycloak getAdminClient(R resource, Context<?> context,
            Keycloak keycloak) {
        Secret adminSecret = context.getClient().resources(Secret.class)
                .inNamespace(resource.getMetadata().getNamespace())
                .withName(keycloak.getMetadata().getName() + "-admin").require();

        String adminUrl = getAdminUrl(keycloak, context);

        Client restEasyClient = null;

        // create a custom client if using https
        if (adminUrl.startsWith(HTTPS)) {
            // TODO: support for mtls
            restEasyClient = createRestEasyClient(resource, context, keycloak, restEasyClient);
        }

        return KeycloakBuilder.builder()
                .serverUrl(adminUrl)
                .realm("master") // TODO: could be configured differently
                // TODO: validate these fields
                .clientId(new String(Base64.getDecoder().decode(adminSecret.getData().get(Constants.CLIENT_ID_KEY)),
                        StandardCharsets.UTF_8))
                .clientSecret(new String(Base64.getDecoder().decode(adminSecret.getData().get(Constants.CLIENT_SECRET_KEY)),
                                StandardCharsets.UTF_8))
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(restEasyClient)
                .build();
    }

    private Client createRestEasyClient(R resource, Context<?> context, Keycloak keycloak, Client restEasyClient) {
        // add server cert trust
        String tlsSecretName = keycloak.getSpec().getHttpSpec().getTlsSecret();
        Secret tlsSecret = context.getClient().resources(Secret.class)
                .inNamespace(resource.getMetadata().getNamespace()).withName(tlsSecretName).require();
        byte[] certBytes = Base64.getDecoder().decode(tlsSecret.getData().get("tls.crt"));

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null);
            ks.setCertificateEntry("cert", cert);
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            ClientBuilder clientBuilder = ClientBuilderWrapper.create(sslContext, false);

            configureHostnameVerifier(context, keycloak, clientBuilder);

            restEasyClient = clientBuilder.build();
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException
                | KeyManagementException e) {
            throw new RuntimeException(e);
        }
        return restEasyClient;
    }

    private void configureHostnameVerifier(Context<?> context, Keycloak keycloak, ClientBuilder clientBuilder) {
        Optional<String> adminHostname = Optional.ofNullable(keycloak.getSpec().getHostnameSpec())
                .map(HostnameSpec::getAdmin)
                .or(() -> Optional.ofNullable(keycloak.getSpec().getHostnameSpec()).map(HostnameSpec::getHostname))
                .map(hostname -> {
                    try {
                        return URI.create("http://"+hostname).getHost();
                    } catch (IllegalArgumentException e) {
                        return hostname;
                    }
                });
        clientBuilder.hostnameVerifier(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                // TODO: validate this approach

                // if we're configured for passthrough, and adminHostname is present
                if (adminHostname.isPresent() && verifier.verify(adminHostname.get(), session)) {
                    return true;
                }
                // otherwise we expect the cert to be valid against the svc hostname - which
                // should happen with service serving certificates
                return verifier.verify(hostname, session);
            }
        });
    }

    private String getAdminUrl(Keycloak keycloak, Context<?> context) {
        boolean httpEnabled = KeycloakServiceDependentResource.isHttpEnabled(keycloak);
        // for now preferring to use http if available
        boolean https = isTlsConfigured(keycloak) && !httpEnabled;
        String protocol = https?HTTPS:"http";
        String address = System.getProperty(KEYCLOAK_TEST_ADDRESS);

        int port = https?HttpSpec.httpsPort(keycloak):HttpSpec.httpPort(keycloak);

        if (address == null) {
            // uses the service host - TODO: assumes the operator and the keycloak instance are in the same cluster
            // this may not eventually hold if we are flexible about where the kube client can target
            address = String.format("%s.%s.svc:%s", KeycloakServiceDependentResource.getServiceName(keycloak),
                    keycloak.getMetadata().getNamespace(), port);
        }

        var relativePath = KeycloakDeploymentDependentResource.readConfigurationValue(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY, keycloak, context)
                .map(path -> !path.isEmpty() && !path.startsWith("/") ? "/" + path : path)
                .orElse("");

        return String.format("%s://%s%s", protocol, address, relativePath);
    }

}
