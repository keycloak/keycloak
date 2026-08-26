package org.keycloak.testframework.remote.timeoffset;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.Time;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

public class TimeOffSet {
    private int currentOffset;
    private final String KEY_OFFSET = "offset";
    private final String CACHES = "caches";
    private final String TIME_OFFSET_ENDPOINT = "/testing-timeoffset";
    private final Object legacyTest;
    private final HttpClient httpClient;
    private final String serverUrl;
    private boolean enableForCaches;

    public TimeOffSet(Object legacyTest) {
        this.legacyTest = Objects.requireNonNull(legacyTest, "legacyTest can not be null");
        this.httpClient = null;
        this.serverUrl = null;
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
            set(currentOffset); // Refresh the server (in case that timeOffset was already set there)
        }
    }

    /**
     * Set the timeoffset on the Keycloak server
     *
     * @param offset the timeoffset
     * @throws RuntimeException
     */
    public void set(int offset) throws RuntimeException {
        currentOffset = offset;

        if (legacyTest != null) {
            setLegacyOffset(offset);
            return;
        }

        setRemoteOffset(offset);
    }

    /**
     * Same as {@link #set(int)} but expecting a {@link Duration}.
     *
     * @param duration the duration
     */
    public void set(Duration duration) {
        Objects.requireNonNull(duration, "duration can not be null");
        set(Math.toIntExact(duration.toSeconds()));
    }

    /**
     * Retrive the current time offset
     *
     * @return the time offset
     */
    public int get() {
        return currentOffset;
    }

    public boolean hasChanged() {
        return currentOffset != 0;
    }

    private void setLegacyOffset(int offset) {
        invokeMethod(legacyTest, "shouldResetTimeOffset", new Class<?>[] { boolean.class }, offset != 0);

        // adminClient depends on Time.offset for auto-refreshing tokens
        Time.setOffset(offset);

        Object testingClient = invokeMethod(legacyTest, "getTestingClient", new Class<?>[] {});
        Object server = invokeMethod(testingClient, "server", new Class<?>[] {});
        RunOnServer runOnServer = session -> {
            Time.setOffset(offset);

            // Time offset was restarted
            if (offset == 0) {
                session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            }
        };
        invokeMethod(server, "run", new Class<?>[] { RunOnServer.class }, runOnServer);

        // force getting new token after time offset has changed
        Object adminClient = invokeMethod(legacyTest, "getAdminClient", new Class<?>[] {});
        Object tokenManager = invokeMethod(adminClient, "tokenManager", new Class<?>[] {});
        invokeMethod(tokenManager, "grantToken", new Class<?>[] {});
    }

    private void setRemoteOffset(int offset) {
        if (httpClient == null || serverUrl == null) {
            throw new IllegalStateException("Remote time offset is not initialized");
        }

        // set for tests
        Time.setOffset(offset);

        // set for KC server
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

    private Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method " + methodName + " on " + target.getClass(), e);
        }
    }
}
