package org.keycloak.ssf.transmitter;

import org.keycloak.models.KeycloakSession;
import org.keycloak.ssf.transmitter.delivery.SecurityEventTokenDispatcher;
import org.keycloak.ssf.transmitter.delivery.poll.PollDeliveryService;
import org.keycloak.ssf.transmitter.delivery.push.PushDeliveryService;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenEncoder;
import org.keycloak.ssf.transmitter.event.SecurityEventTokenMapper;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.stream.StreamVerificationService;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.ssf.transmitter.subject.SsfSubjectInclusionResolver;
import org.keycloak.ssf.transmitter.subject.SubjectManagementService;
import org.keycloak.ssf.transmitter.support.SsfPushUrlValidator;

/**
 * Extension seam for constructing the per-session services that make
 * up an {@link SsfTransmitterProvider}. Default implementation lives
 * on {@link DefaultSsfTransmitterProviderFactory}; deployments that
 * need to swap a single service (e.g. plug in a custom
 * {@link SecurityEventTokenEncoder}) override one method instead of
 * subclassing the entire provider.
 *
 * <h3>Two flavours of factory methods</h3>
 *
 * <ul>
 *     <li><b>Leaf services</b> — {@code createMapper},
 *         {@code createEncoder}, {@code createPushDelivery},
 *         {@code createStreamStore}, {@code createMetadataService},
 *         {@code createSubjectManagement} — take only
 *         {@code (session, context)}. They have no dependency on
 *         other lazy-cached services.</li>
 *     <li><b>Composite services</b> — {@code createDispatcher},
 *         {@code createVerification}, {@code createPollDelivery} — take
 *         the {@link SsfTransmitterProvider} so they can pull cached
 *         dependencies (mapper, encoder, push delivery) via the
 *         provider's accessors instead of building fresh, redundant
 *         instances. The provider exposes {@link
 *         SsfTransmitterProvider#session() session()} and {@link
 *         SsfTransmitterProvider#context() context()} on the
 *         interface so the composite builders don't need to downcast
 *         to the default impl.</li>
 * </ul>
 */
public interface SsfTransmitterServiceBuilder {

    SecurityEventTokenEncoder createEncoder(KeycloakSession session, SsfTransmitterContext ctx);

    SecurityEventTokenMapper createMapper(KeycloakSession session, SsfTransmitterContext ctx);

    PushDeliveryService createPushDelivery(KeycloakSession session, SsfTransmitterContext ctx);

    ClientStreamStore createStreamStore(KeycloakSession session, SsfTransmitterContext ctx);

    TransmitterMetadataService createMetadataService(KeycloakSession session, SsfTransmitterContext ctx);

    SubjectManagementService createSubjectManagement(KeycloakSession session, SsfTransmitterContext ctx);

    SsfSubjectInclusionResolver createSubjectInclusionResolver(KeycloakSession session, SsfTransmitterContext ctx);

    SecurityEventTokenDispatcher createDispatcher(SsfTransmitterProvider provider);

    StreamVerificationService createVerification(SsfTransmitterProvider provider);

    PollDeliveryService createPollDelivery(SsfTransmitterProvider provider);

    default SsfPushUrlValidator createPushUrlValidator(SsfTransmitterConfig config) {
        return new SsfPushUrlValidator(config.isAllowInsecurePushTargets());
    }
}
