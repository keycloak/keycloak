package org.keycloak.ssf.transmitter.subject;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;

/**
 * Read-side gate that decides whether a user / organization counts as a
 * subscribed subject for a given receiver. Drives the dispatcher's
 * subject-selection filter and the synthetic-emit dispatchability
 * check.
 *
 * <p>The default implementation
 * ({@link DefaultSsfSubjectInclusionResolver}) reads the
 * {@code ssf.notify.<receiverClientId>} attribute on the user and the
 * user's owning organization via {@link SsfNotifyAttributes}.
 * Extensions can subclass it to layer additional sources (group
 * attribute lookups, role-based opt-ins, external policy services) on
 * top of the default behaviour, e.g.
 *
 * <pre>{@code
 * @Override
 * public boolean isUserNotified(KeycloakSession s, UserModel u, String c) {
 *     return super.isUserNotified(s, u, c) || isInNotifyGroup(u, c);
 * }
 * }</pre>
 *
 * <p>Resolved per-session via
 * {@link org.keycloak.ssf.transmitter.SsfTransmitterProvider#subjectInclusionResolver()}.
 * No standalone Keycloak SPI registration — extensions plug in by
 * overriding the transmitter provider's
 * {@code subjectInclusionResolver()} accessor or by supplying a custom
 * {@link org.keycloak.ssf.transmitter.SsfTransmitterServiceBuilder}.
 *
 * <p>Tombstone reads (the {@code ssf.notifyRemovedAt.*} attribute used
 * for the SSF §9.3 grace window) are intentionally NOT part of this
 * interface: those are timestamp lookups tied to the writer side
 * ({@link SsfNotifyAttributes#stampRemovedAtForUser}), not subscription
 * opinions, so they stay on the static helper.
 */
public interface SsfSubjectInclusionResolver {

    /**
     * Whether the user should be treated as explicitly notified for
     * this receiver. Drives the {@code default_subjects=NONE} include
     * check — returning {@code true} delivers the event even when the
     * stream is opt-in by default.
     */
    boolean isUserNotified(KeycloakSession session, UserModel user, String receiverClientId);

    /**
     * Whether the user should be treated as explicitly excluded for
     * this receiver. Drives the {@code default_subjects=ALL} skip
     * check — returning {@code true} drops the event even when the
     * stream is broadcast by default.
     */
    boolean isUserExcluded(KeycloakSession session, UserModel user, String receiverClientId);

    /**
     * Org-level analog of {@link #isUserNotified}. Called per
     * organization the user belongs to; any one matching org makes the
     * subject notified.
     */
    boolean isOrganizationNotified(KeycloakSession session, OrganizationModel organization, String receiverClientId);

    /**
     * Org-level analog of {@link #isUserExcluded}. Called per
     * organization the user belongs to; any one matching org excludes
     * the subject.
     */
    boolean isOrganizationExcluded(KeycloakSession session, OrganizationModel organization, String receiverClientId);
}
