/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.health.site;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.keycloak.models.ModelException;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.security.BasicCallbackHandler;
import org.infinispan.client.hotrod.security.TokenCallbackHandler;
import org.infinispan.client.rest.RestClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.client.rest.RestResponseInfo;
import org.infinispan.client.rest.configuration.RestClientConfigurationBuilder;
import org.infinispan.commons.dataconversion.internal.Json;
import org.infinispan.commons.util.concurrent.CompletionStages;

/**
 * {@link InfinispanManagement} implementation that uses the Infinispan REST API to query and control cross-site
 * replication state.
 * <p>
 * The {@link #fromRemoteCacheManager(RemoteCacheManager)} factory method creates a REST client by copying the
 * connection configuration (host, authentication, and TLS) from an existing Hot Rod client, so that no additional
 * server coordinates need to be configured.
 */
public class RestInfinispanManagement implements InfinispanManagement {

    private final RestClient restClient;

    public static RestInfinispanManagement fromRemoteCacheManager(RemoteCacheManager remoteCacheManager) {
        var restConfigBuilder = new RestClientConfigurationBuilder();
        var hotRodConfig = remoteCacheManager.getConfiguration();
        restConfigBuilder.pingOnCreate(false)
                .followRedirects(true)
                .tcpKeepAlive(false)
                .socketTimeout(hotRodConfig.socketTimeout());
        copyHostName(hotRodConfig, restConfigBuilder);
        copyAuthentication(hotRodConfig, restConfigBuilder);
        copySsl(hotRodConfig, restConfigBuilder);
        var restClient = RestClient.forConfiguration(restConfigBuilder.build());
        return new RestInfinispanManagement(restClient);
    }

    public RestInfinispanManagement(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    @Override
    public Map<String, String> siteStatus() throws ExecutionException, InterruptedException {
        CompletionStage<Map<String, String>> stage = restClient.container().backupStatuses()
                .thenApply(restResponse -> {
                    checkResponse(restResponse);
                    var data = Json.read(restResponse.body()).asMap();
                    if (data.isEmpty()) {
                        return Map.of();
                    }
                    return data.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> Json.make(entry.getValue()).at("status").asString()));
                });
        return CompletionStages.await(stage);
    }

    @Override
    public SiteConnection siteConnection() throws ExecutionException, InterruptedException {
        var stage = restClient.container().info()
                .thenApply(restResponse -> {
                    checkResponse(restResponse);
                    var data = Json.read(restResponse.body());
                    var localSite = data.at("local_site").isNull() ? null : data.at("local_site").asString();
                    var sites = data.at("sites_view").asJsonList().stream().map(Json::asString).collect(Collectors.toSet());
                    return new SiteConnection(localSite, sites);
                });
        return CompletionStages.await(stage);
    }

    @Override
    public void disconnect(Collection<String> remoteSites) throws ExecutionException, InterruptedException {
        if (remoteSites.isEmpty()) {
            return;
        }
        var container = restClient.container();
        var stage = CompletionStages.aggregateCompletionStage();
        remoteSites.stream()
                .distinct()
                .map(container::takeOffline)
                .forEach(stage::dependsOn);
        CompletionStages.await(stage.freeze());
    }

    @Override
    public void close() throws Exception {
        restClient.close();
    }

    private static void checkResponse(RestResponse response) {
        if (response == null) {
            throw new IllegalStateException("Null response received from external Infinispan cluster");
        }
        if (response.status() != RestResponseInfo.OK) {
            throw new ModelException("Unexpected http response status. Expected %s but got %s", RestResponseInfo.OK, response.status());
        }
    }

    private static void copyHostName(org.infinispan.client.hotrod.configuration.Configuration from, RestClientConfigurationBuilder to) {
        var server = from.servers().get(0);
        to.addServer().host(server.host()).port(server.port());
    }

    private static void copyAuthentication(org.infinispan.client.hotrod.configuration.Configuration from, RestClientConfigurationBuilder to) {
        var authn = from.security().authentication();
        if (!authn.enabled()) {
            return;
        }
        to.security().authentication().enable()
                .clientSubject(authn.clientSubject())
                .mechanism("AUTO");
        var callback = authn.callbackHandler();
        if (callback instanceof BasicCallbackHandler bch) {
            to.security().authentication()
                    .username(bch.getUsername())
                    .password(bch.getPassword())
                    .realm(bch.getRealm());
        } else if (callback instanceof TokenCallbackHandler tch) {
            to.security().authentication().username(tch.getToken());
        }
    }

    private static void copySsl(org.infinispan.client.hotrod.configuration.Configuration from, RestClientConfigurationBuilder to) {
        var ssl = from.security().ssl();
        if (!ssl.enabled()) {
            return;
        }
        to.security().ssl()
                .enable()
                .sslContext(ssl.sslContext())
                .sniHostName(ssl.sniHostName());
    }
}
