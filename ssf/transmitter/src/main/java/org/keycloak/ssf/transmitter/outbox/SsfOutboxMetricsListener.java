package org.keycloak.ssf.transmitter.outbox;

import java.util.List;
import java.util.function.Function;

import org.keycloak.events.outbox.OutboxEntry;
import org.keycloak.events.outbox.OutboxMetricsListener;
import org.keycloak.events.outbox.OutboxStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.transmitter.metrics.SsfMetricsBinder;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * SSF flavour of the generic {@link OutboxMetricsListener}. The
 * generic {@code keycloak.outbox.*} meters (tick, transitions,
 * dead-letter, depth) come from the base class; this subclass adds
 * what is SSF-specific:
 *
 * <ul>
 *   <li>The depth snapshot spans both SSF kinds, push and poll, so the
 *       backlog of poll receivers is visible although only push rows
 *       are drained.</li>
 *   <li>A dead-lettered push row is also counted on the per-receiver
 *       {@code keycloak.ssf.push.delivery{outcome="dead_letter"}}
 *       meter, with the same realm-name / clientId labels the
 *       {@link SsfPushDeliveryHandler} uses for the per-attempt
 *       outcomes. SSF receivers are few per realm, so the owner label
 *       is affordable here.</li>
 * </ul>
 */
public class SsfOutboxMetricsListener extends OutboxMetricsListener {

    protected final SsfMetricsBinder metricsBinder;

    public SsfOutboxMetricsListener(SsfMetricsBinder metricsBinder,
                                    Function<KeycloakSession, OutboxStore> storeFactory) {
        super(SsfOutboxKinds.PUSH, storeFactory);
        this.metricsBinder = metricsBinder == null ? SsfMetricsBinder.NOOP : metricsBinder;
    }

    public SsfOutboxMetricsListener(SsfMetricsBinder metricsBinder,
                                    Function<KeycloakSession, OutboxStore> storeFactory,
                                    MeterRegistry registry) {
        super(SsfOutboxKinds.PUSH, storeFactory, registry);
        this.metricsBinder = metricsBinder == null ? SsfMetricsBinder.NOOP : metricsBinder;
    }

    @Override
    protected List<String> depthKinds() {
        return List.of(SsfOutboxKinds.PUSH, SsfOutboxKinds.POLL);
    }

    @Override
    public void onDeadLetter(KeycloakSession session, OutboxEntry row, DeadLetterCause cause, String reason) {
        super.onDeadLetter(session, row, cause, reason);
        metricsBinder.recordPushDeadLetter(realmLabel(session, row.getRealmId()), clientLabel(session, row));
    }

    /**
     * The receiver's clientId, falling back to the raw owner id when
     * the realm or client no longer exists (orphaned rows).
     */
    protected String clientLabel(KeycloakSession session, OutboxEntry row) {
        if (session != null) {
            RealmModel realm = session.realms().getRealm(row.getRealmId());
            if (realm != null) {
                ClientModel client = realm.getClientById(row.getOwnerId());
                if (client != null) {
                    return client.getClientId();
                }
            }
        }
        return row.getOwnerId();
    }
}
