package org.keycloak.protocol.ssf.receiver.management;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.SsfException;
import org.keycloak.protocol.ssf.keys.TransmitterKeyProviderFactory;
import org.keycloak.protocol.ssf.keys.TransmitterPublicKeyLoader;
import org.keycloak.protocol.ssf.receiver.ReceiverConfig;
import org.keycloak.protocol.ssf.receiver.ReceiverKeyModel;
import org.keycloak.protocol.ssf.receiver.ReceiverModel;
import org.keycloak.protocol.ssf.receiver.SsfReceiver;
import org.keycloak.protocol.ssf.receiver.SsfReceiverFactory;
import org.keycloak.protocol.ssf.receiver.transmitterclient.SsfTransmitterClient;
import org.keycloak.protocol.ssf.spi.SsfProvider;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterMetadata;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReceiverManager {

    protected static final Logger log = Logger.getLogger(ReceiverManager.class);

    private final KeycloakSession session;

    public ReceiverManager(KeycloakSession session) {
        this.session = session;
    }

    public ReceiverModel createOrUpdateReceiver(KeycloakContext context, String receiverAlias, ReceiverConfig receiverConfig) {

        RealmModel realm = context.getRealm();

        String componentId = createReceiverComponentId(realm, receiverAlias);

        ComponentModel existingComponent = realm.getComponent(componentId);
        ReceiverModel receiverModel;
        if (existingComponent == null) {
            log.infof("Creating new receiver. realm=%s alias=%s", realm.getName(), receiverAlias);
            receiverModel = ReceiverModel.create(receiverAlias, receiverConfig);
            receiverModel.setId(componentId);
            receiverModel.setParentId(realm.getId());
            receiverModel.setName(receiverAlias);
            String providerId = Optional.ofNullable(receiverModel.getProviderId()).orElse("default");
            receiverModel.setProviderId(providerId);
            receiverModel.setProviderType(SsfReceiver.class.getName());

            realm.addComponentModel(receiverModel);
        } else {
            receiverModel = new ReceiverModel(existingComponent);
            log.infof("Updating existing receiver. realm=%s alias=%s stream_id=%s", realm.getName(), receiverAlias, receiverModel.getStreamId());
        }

        SsfReceiver receiver = lookupReceiver(context, receiverAlias);
        registerKeys(receiverModel);

        if (Boolean.TRUE.equals(receiverModel.getManagedStream())) {
            try {
                receiverModel = receiver.registerStream();
                log.debugf("Registered receiver with managed stream. realm=%s alias=%s stream_id=%s", realm.getName(), receiverModel.getAlias(), receiverModel.getStreamId());
            } catch (final SsfException e) {
                removeReceiver(context, receiverModel);
                throw e;
            }
        } else {
            receiverModel = receiver.importStream();
            log.debugf("Registered receiver with pre-configured stream. realm=%s alias=%s stream_id=%s", realm.getName(), receiverModel.getAlias(), receiverModel.getStreamId());
        }

        updateReceiverModel(realm, receiverModel);

        return receiverModel;
    }

    protected void updateReceiverModel(RealmModel realm, ReceiverModel model) {

        model.setModifiedAt(Time.currentTimeMillis());
        int hash = ReceiverModel.computeConfigHash(model);
        model.setConfigHash(hash);

        realm.updateComponent(model);
    }

    protected ReceiverModel importStreamMetadata(ReceiverModel model) {
        SsfReceiver receiver = lookupReceiver(model);
        receiver.importStream();
        return receiver.getReceiverModel();
    }

    public void registerKeys(ReceiverModel receiverModel) {

        SsfProvider sharedSignals = session.getProvider(SsfProvider.class);
        SsfTransmitterClient ssfTransmitterClient = sharedSignals.transmitterClient();

        SsfTransmitterMetadata transmitterMetadata = ssfTransmitterClient.loadTransmitterMetadata(receiverModel);

        receiverModel.setIssuer(transmitterMetadata.getIssuer());
        receiverModel.setJwksUri(transmitterMetadata.getJwksUri());

        refreshKeys(session.getContext(), receiverModel, transmitterMetadata);
    }

    protected void refreshKeys(KeycloakContext context, ReceiverModel receiverModel, SsfTransmitterMetadata transmitterMetadata) {
        RealmModel realm = context.getRealm();
        TransmitterPublicKeyLoader publicKeyLoader = new TransmitterPublicKeyLoader(session, transmitterMetadata);
        try {
            PublicKeysWrapper publicKeysWrapper = publicKeyLoader.loadKeys();
            List<KeyWrapper> keys = publicKeysWrapper.getKeys();
            log.debugf("Fetched %s receiver keys from JWKS url. realm=%s receiver=%s url=%s", keys.size(), realm.getName(), receiverModel.getAlias(), transmitterMetadata.getJwksUri());
            for (var key : keys) {
                createOrUpdateReceiverKey(receiverModel, key, realm);
            }
        } catch (Exception e) {
            throw new SsfException("Failed to load public keys from transmitter JWKS endpoint", e);
        }
    }

    private static void createOrUpdateReceiverKey(ReceiverModel receiverModel, KeyWrapper key, RealmModel realm) {
        String receiverKeyComponentId = createReceiverKeyComponentId(receiverModel, key.getKid());

        ReceiverKeyModel receiverKeyModel;
        ComponentModel existing = realm.getComponent(receiverKeyComponentId);
        if (existing != null) {
            receiverKeyModel = new ReceiverKeyModel(existing);
        } else {
            receiverKeyModel = new ReceiverKeyModel();
            receiverKeyModel.setId(receiverKeyComponentId);
            receiverKeyModel.setParentId(receiverModel.getId());
            receiverKeyModel.setProviderType(KeyProvider.class.getName());
            receiverKeyModel.setProviderId(TransmitterKeyProviderFactory.PROVIDER_ID);
            String receiverKeyModelName = receiverModel.getName() + " Key Provider " + key.getKid();
            receiverKeyModel.setName(receiverKeyModelName);
        }

        receiverKeyModel.setKid(key.getKid());
        receiverKeyModel.setAlgorithm(key.getAlgorithm());
        receiverKeyModel.setKeyUse(key.getUse());
        receiverKeyModel.setType(key.getType());

        // store public key
        String encodedPublicKey = Base64.getEncoder().encodeToString(key.getPublicKey().getEncoded());
        receiverKeyModel.setPublicKey(encodedPublicKey);

        if (existing == null) {
            realm.addComponentModel(receiverKeyModel);
            log.debugf("Registered receiver key component. realm=%s receiver=%s name='%s'", realm.getName(), receiverModel.getAlias(), receiverKeyModel.getName());
        } else {
            realm.updateComponent(receiverKeyModel);
            log.debugf("Updated receiver key component. realm=%s receiver=%s name='%s'", realm.getName(), receiverModel.getAlias(), receiverKeyModel.getName());
        }
    }

    public void removeAllReceivers(RealmModel realm) {
        listReceivers(realm).forEach(receiverModel -> {
            removeReceiver(realm, receiverModel);
        });
    }

    public void removeReceiver(KeycloakContext context, ReceiverModel receiverModel) {
        removeReceiver(context.getRealm(), receiverModel);
    }

    public void removeReceiver(RealmModel realm, ReceiverModel receiverModel) {

        SsfReceiver receiver = lookupReceiver(receiverModel);
        if (receiver == null) {
            return;
        }

        ReceiverModel model = receiver.getReceiverModel();

        if (receiverModel.getStreamId() == null) {
            log.debugf("Skipping unregister stream for unknown streamId. realm=%s receiver=%s", realm.getName(), model.getAlias());
        } else {
            // only remove stream if we stored a stream id
            receiver.unregisterStream();
        }

        unregisterKeys(realm, model);

        realm.removeComponent(model);
        log.debugf("Removed receiver component with id %s. realm=%s receiver=%s", model.getId(), realm.getName(), model.getAlias());
    }

    public void unregisterKeys(RealmModel realm, ReceiverModel model) {

        for (ComponentModel receiverKeyModel : realm.getComponentsStream(model.getId(), TransmitterKeyProviderFactory.PROVIDER_ID).toList()) {
            realm.removeComponent(receiverKeyModel);
            log.debugf("Removed %s receiver key component with id %s. realm=%s receiver=%s", receiverKeyModel.getName(), receiverKeyModel.getId(), realm.getName(), model.getAlias());
        }
    }

    public SsfReceiver lookupReceiver(KeycloakContext context, String receiverAlias) {

        ReceiverModel receiverModel = getReceiverModel(context, receiverAlias);
        if (receiverModel == null) {
            return null;
        }
        return lookupReceiver(receiverModel);
    }

    public SsfReceiver lookupReceiver(ReceiverModel receiverModel) {

        KeycloakSessionFactory ksf = session.getKeycloakSessionFactory();
        SsfReceiverFactory receiverFactory = (SsfReceiverFactory) ksf.getProviderFactory(SsfReceiver.class);
        if (receiverFactory == null) {
            return null;
        }

        SsfReceiver receiver = receiverFactory.create(session, receiverModel);
        return receiver;
    }


    public static String createReceiverComponentId(RealmModel realm, String receiverAlias) {
        String componentId = UUID.nameUUIDFromBytes((realm.getId() + receiverAlias).getBytes()).toString();
        return componentId;
    }

    public static String createReceiverKeyComponentId(ReceiverModel model, String kid) {
        String componentId = UUID.nameUUIDFromBytes((model.getId() + "::" + kid).getBytes()).toString();
        return componentId;
    }

    public List<ReceiverModel> listReceivers(KeycloakContext context) {

        RealmModel realm = context.getRealm();
        return listReceivers(realm);
    }

    public List<ReceiverModel> listReceivers(RealmModel realm) {
        List<ReceiverModel> receiverModels = realm
                .getComponentsStream(realm.getId(), SsfReceiver.class.getName())
                .map(ReceiverModel::new)
                .toList();

        return receiverModels;
    }

    public ReceiverModel getReceiverModel(KeycloakContext context, String alias) {
       return getReceiverModel(context.getRealm(), alias);
    }

    public ReceiverModel getReceiverModel(RealmModel realm, String alias) {
        String componentId = createReceiverComponentId(realm, alias);
        ComponentModel component = realm.getComponent(componentId);
        if (component != null) {
            return new ReceiverModel(component);
        }
        return null;
    }

    public void refreshReceiver(KeycloakContext context, ReceiverModel receiverModel) {

        SsfTransmitterMetadata transmitterMetadata = refreshTransmitterMetadata(receiverModel);
        refreshKeys(context, receiverModel, transmitterMetadata);
        ReceiverModel updatedModel = refreshStream(receiverModel);

        RealmModel realm = context.getRealm();
        updateReceiverModel(realm, updatedModel);

        log.debugf("Refreshed receiver model. realm=%s receiver=%s", realm.getName(), receiverModel.getAlias());
    }

    public ReceiverModel refreshStream(ReceiverModel receiverModel) {
        ReceiverModel updatedModel = importStreamMetadata(receiverModel);
        return updatedModel;
    }

    public SsfTransmitterMetadata refreshTransmitterMetadata(ReceiverModel receiverModel) {

        SsfReceiver receiver = lookupReceiver(receiverModel);
        if (receiver == null) {
            return null;
        }

        return receiver.refreshTransmitterMetadata();
    }
}
