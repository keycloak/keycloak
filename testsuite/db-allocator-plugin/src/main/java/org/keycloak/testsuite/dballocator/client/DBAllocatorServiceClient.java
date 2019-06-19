package org.keycloak.testsuite.dballocator.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.keycloak.testsuite.dballocator.client.data.AllocationResult;
import org.keycloak.testsuite.dballocator.client.data.EraseResult;
import org.keycloak.testsuite.dballocator.client.data.ReleaseResult;
import org.keycloak.testsuite.dballocator.client.exceptions.DBAllocatorException;
import org.keycloak.testsuite.dballocator.client.retry.IncrementalBackoffRetryPolicy;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DBAllocatorServiceClient {

    private static final int TIMEOUT = 10_000;

    private final Client restClient;
    private final URI allocatorServletURI;
    private final BackoffRetryPolicy retryPolicy;

    public DBAllocatorServiceClient(String allocatorServletURI, BackoffRetryPolicy retryPolicy) {
        Objects.requireNonNull(allocatorServletURI, "DB Allocator URI must not be null");

        this.allocatorServletURI = URI.create(allocatorServletURI);
        this.retryPolicy = retryPolicy != null ? retryPolicy : new IncrementalBackoffRetryPolicy();
        this.restClient = new ResteasyClientBuilder().httpEngine(createEngine()).build();
    }

    private final ApacheHttpClient43Engine createEngine() {
        RequestConfig reqConfig = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .setConnectionRequestTimeout(TIMEOUT)
                .build();
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(reqConfig)
                .setMaxConnTotal(1)
                .build();

        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        engine.setFollowRedirects(true);
        return engine;
    }

    public AllocationResult allocate(String user, String type, int expiration, TimeUnit expirationTimeUnit, String location) throws DBAllocatorException {
        Objects.requireNonNull(user, "User can not be null");
        Objects.requireNonNull(type, "DB Type must not be null");

        try {
            String typeWithLocation = location != null ? type + "&&" + location : type;
            Invocation.Builder target = restClient
                    .target(allocatorServletURI)
                    .queryParam("operation", "allocate")
                    .queryParam("requestee", user)
                    .queryParam("expression", typeWithLocation)
                    .queryParam("expiry", expirationTimeUnit.toMinutes(expiration))
                    .request();

            Response response = retryPolicy.retryTillHttpOk(() -> target.get());
            Properties properties = new Properties();
            String content = response.readEntity(String.class);

            if (content != null) {
                try(InputStream is = new ByteArrayInputStream(content.getBytes())) {
                    properties.load(is);
                }
            }

            return AllocationResult.successful(properties);
        } catch (IOException e) {
            throw new DBAllocatorException(e);
        }
    }

    public EraseResult erase(AllocationResult allocationResult) throws DBAllocatorException {
        Objects.requireNonNull(allocationResult, "Previous allocation result must not be null");
        Objects.requireNonNull(allocationResult.getUUID(), "UUID must not be null");

        Invocation.Builder target = restClient
                .target(allocatorServletURI)
                .queryParam("operation", "erase")
                .queryParam("uuid", allocationResult.getUUID())
                .request();

        try (Response response = retryPolicy.retryTillHttpOk(() -> target.get())) {
            return EraseResult.successful(allocationResult.getUUID());
        }
    }

    public ReleaseResult release(AllocationResult allocationResult) throws DBAllocatorException {
        Objects.requireNonNull(allocationResult, "Previous allocation result must not be null");
        Objects.requireNonNull(allocationResult.getUUID(), "UUID must not be null");

        Invocation.Builder target = restClient
                .target(allocatorServletURI)
                .queryParam("operation", "dealloc")
                .queryParam("uuid", allocationResult.getUUID())
                .request();

        try (Response response = retryPolicy.retryTillHttpOk(() -> target.get())) {
            return ReleaseResult.successful(allocationResult.getUUID());
        }
    }
}
