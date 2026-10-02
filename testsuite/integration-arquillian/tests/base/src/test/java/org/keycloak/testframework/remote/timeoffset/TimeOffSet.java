package org.keycloak.testframework.remote.timeoffset;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.testsuite.AbstractKeycloakTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

public class TimeOffSet {

    private static final String KEY_OFFSET = "offset";
    private static final String CACHES = "caches";
    private static final String TIME_OFFSET_ENDPOINT = "/testing-timeoffset";

    private int currentOffset;
    private final AbstractKeycloakTest legacyTest;
    private final HttpClient httpClient;
    private final String serverUrl;
    private boolean enableForCaches;

    public TimeOffSet(AbstractKeycloakTest test) {
        this.legacyTest = test;
        this.httpClient = null;
        this.serverUrl = null;
    }

    public TimeOffSet(Object test) {
        this((AbstractKeycloakTest) test);
    }

    public TimeOffSet(HttpClient httpClient, String serverUrl, int initOffset, boolean enableForCaches) {
        this.legacyTest = null;
        this.httpClient = httpClient;
        this.serverUrl = serverUrl;
        this.enableForCaches = enableForCaches;
        if (initOffset != 0) {
            set(initOffset);
        }
        currentOffset = initOffset;
    }

    public void enableForCaches() {
        this.enableForCaches = true;
        if (legacyTest == null && currentOffset != 0) {
            set(currentOffset);
        }
    }

    public void set(int offset) {
        currentOffset = offset;
        if (legacyTest != null) {
            setLegacyOffset(offset);
            return;
        }
        setRemoteOffset(offset);
    }

    public void set(Duration duration) {
        Objects.requireNonNull(duration, "duration can not be null");
        set(Math.toIntExact(duration.toSeconds()));
    }

    public int get() {
        return currentOffset;
    }

    public boolean hasChanged() {
        return currentOffset != 0;
    }

    private void setLegacyOffset(int offset) {
        legacyTest.shouldResetTimeOffset(offset != 0);
        Time.setOffset(offset);
        legacyTest.getTestingClient().server().run(session -> {
            Time.setOffset(offset);
            if (offset == 0) {
                session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            }
        });
        legacyTest.getAdminClient().tokenManager().grantToken();
    }

    private void setRemoteOffset(int offset) {
        if (httpClient == null || serverUrl == null) {
            throw new IllegalStateException("Remote time offset is not initialized");
        }
        Time.setOffset(offset);
        var time = Map.of(KEY_OFFSET, offset, CACHES, enableForCaches);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(time);
            HttpPut request = new HttpPut(serverUrl + TIME_OFFSET_ENDPOINT);
            request.setEntity(new StringEntity(json));
            request.setHeader("Content-type", "application/json");
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != Response.Status.OK.getStatusCode()) {
                var statusLine = response.getStatusLine();
                throw new WebApplicationException(String.format("Unexpected response status for TimeOffSet: %d %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
