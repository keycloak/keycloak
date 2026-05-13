package org.keycloak.ssf.transmitter.subject;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.UserModel;

/**
 * Stateless default {@link SsfSubjectInclusionResolver} that delegates
 * to the {@link SsfNotifyAttributes} static helpers — i.e. reads the
 * {@code ssf.notify.<receiverClientId>} attribute on the user / org.
 *
 * <p>Subclass and override individual methods to layer additional
 * inclusion sources on top of the attribute check (typically with
 * {@code super.isXxx(...) || extraCheck(...)}).
 */
public class DefaultSsfSubjectInclusionResolver implements SsfSubjectInclusionResolver {

    @Override
    public boolean isUserNotified(KeycloakSession session, UserModel user, String receiverClientId) {
        return SsfNotifyAttributes.isUserNotified(user, receiverClientId);
    }

    @Override
    public boolean isUserExcluded(KeycloakSession session, UserModel user, String receiverClientId) {
        return SsfNotifyAttributes.isUserExcluded(user, receiverClientId);
    }

    @Override
    public boolean isOrganizationNotified(KeycloakSession session, OrganizationModel organization, String receiverClientId) {
        return SsfNotifyAttributes.isOrganizationNotified(organization, receiverClientId);
    }

    @Override
    public boolean isOrganizationExcluded(KeycloakSession session, OrganizationModel organization, String receiverClientId) {
        return SsfNotifyAttributes.isOrganizationExcluded(organization, receiverClientId);
    }
}
