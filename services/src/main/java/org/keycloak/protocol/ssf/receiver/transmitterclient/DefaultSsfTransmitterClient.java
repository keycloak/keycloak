package org.keycloak.protocol.ssf.receiver.transmitterclient;

import org.jboss.logging.Logger;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.ssf.SsfException;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultSsfTransmitterClient implements SsfTransmitterClient {

    protected static final Logger log = Logger.getLogger(DefaultSsfTransmitterClient.class);

    private final KeycloakSession session;

    public DefaultSsfTransmitterClient(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SsfTransmitterMetadata loadTransmitterMetadata(ReceiverModel receiverModel) {

        SsfTransmitterMetadata metadata = loadFromCache(receiverModel);

        if (metadata != null) {
            return metadata;
        }

        metadata = fetchTransmitterMetadata(receiverModel);

        if (metadata != null) {
            storeToCache(receiverModel, metadata);
        }

        return metadata;
    }

    @Override
    public SsfTransmitterMetadata fetchTransmitterMetadata(ReceiverModel receiverModel) {

        RealmModel realm = session.getContext().getRealm();
        String url = receiverModel.getTransmitterConfigUrl();

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

    protected void storeToCache(ReceiverModel receiverModel, SsfTransmitterMetadata metadata) {

        RealmModel realm = session.getContext().getRealm();
        String url = receiverModel.getTransmitterConfigUrl();

        SingleUseObjectProvider cache = session.getProvider(SingleUseObjectProvider.class);
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

    protected SsfTransmitterMetadata loadFromCache(ReceiverModel receiverModel) {

        String url = receiverModel.getTransmitterConfigUrl();
        // TODO cache transmitter metadata
        SingleUseObjectProvider cache = session.getProvider(SingleUseObjectProvider.class);
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

    @Override
    public boolean clearTransmitterMetadata(ReceiverModel receiverModel) {

        SingleUseObjectProvider cache = session.getProvider(SingleUseObjectProvider.class);
        String cacheKey = makeCacheKey(receiverModel.getTransmitterConfigUrl());
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
