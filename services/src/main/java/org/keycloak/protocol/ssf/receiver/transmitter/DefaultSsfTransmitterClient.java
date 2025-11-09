package org.keycloak.protocol.ssf.receiver.transmitter;

import org.jboss.logging.Logger;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.ssf.SsfException;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultSsfTransmitterClient implements SsfTransmitterClient {

    protected static final Logger log = Logger.getLogger(DefaultSsfTransmitterClient.class);

    protected final KeycloakSession session;

    public DefaultSsfTransmitterClient(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SsfTransmitterMetadata loadTransmitterMetadata(SsfReceiver receiver) {

        SsfTransmitterMetadata metadata = loadFromCache(receiver);

        if (metadata != null) {
            return metadata;
        }

        metadata = fetchTransmitterMetadata(receiver);

        if (metadata != null) {
            storeToCache(receiver, metadata);
        }

        return metadata;
    }

    @Override
    public SsfTransmitterMetadata fetchTransmitterMetadata(SsfReceiver receiver) {

        RealmModel realm = session.getContext().getRealm();
        String url = receiver.getTransmitterConfigUrl();

        log.debugf("Sending transmitter metadata request. realm=%s url=%s", realm.getName(), url);
        var request = createHttpClient().doGet(url);
        try (var response = request.asResponse()) {
            log.debugf("Received transmitter metadata response. realm=%s status=%s", realm.getName(), response.getStatus());
            if (response.getStatus() != 200) {
                throw new SsfException("Expected a 200 response but got: " + response.getStatus());
            }
            SsfTransmitterMetadata metadata = response.asJson(SsfTransmitterMetadata.class);
            return metadata;
        } catch (Exception e) {
            throw new SsfException("Could fetch transmitter metadata", e);
        }
    }

    protected void storeToCache(SsfReceiver receiver, SsfTransmitterMetadata metadata) {

        RealmModel realm = session.getContext().getRealm();
        String url = receiver.getTransmitterConfigUrl();

        SingleUseObjectProvider cache = getCache();
        try {
            String jsonData = JsonSerialization.writeValueAsString(metadata);
            cache.put(makeCacheKey(url), getCacheLifespanSeconds(), Map.of("data", jsonData));
            log.debugf("Stored transmitter metadata in cache. realm=%s url=%s", realm.getName(), url);
        } catch (IOException e) {
            throw new SsfException("Could not store transmitter metadata in cache", e);
        }
    }

    protected long getCacheLifespanSeconds() {
        return TimeUnit.HOURS.toSeconds(12);
    }

    protected SsfTransmitterMetadata loadFromCache(SsfReceiver receiver) {

        String url = receiver.getTransmitterConfigUrl();

        SingleUseObjectProvider cache = getCache();
        Map<String, String> cachedTransmitterMetadata = cache.get(makeCacheKey(url));
        if (cachedTransmitterMetadata != null) {
            String jsonData = cachedTransmitterMetadata.get("data");
            try {
                RealmModel realm = session.getContext().getRealm();
                SsfTransmitterMetadata metadata = JsonSerialization.readValue(jsonData, SsfTransmitterMetadata.class);
                log.debugf("Loaded transmitter metadata from cache. realm=%s url=%s", realm.getName(), url);
                return metadata;
            } catch (IOException e) {
                throw new SsfException("Could load transmitter metadata from cache", e);
            }
        }

        return null;
    }

    protected SingleUseObjectProvider getCache() {
        return session.getProvider(SingleUseObjectProvider.class);
    }

    @Override
    public boolean clearTransmitterMetadata(SsfReceiver receiver) {

        SingleUseObjectProvider cache = getCache();
        String cacheKey = makeCacheKey(receiver.getTransmitterConfigUrl());
        Map<String, String> cachedTransmitterMetadata = cache.get(cacheKey);
        if (cachedTransmitterMetadata != null) {
            cache.remove(cacheKey);
            return true;
        }
        return false;
    }

    protected String makeCacheKey(String url) {
        RealmModel realm = session.getContext().getRealm();
        return "ssf:tm:" + realm.getName() + ":" + url.hashCode();
    }

    protected SimpleHttp createHttpClient() {
        return SimpleHttp.create(session);
    }
}
