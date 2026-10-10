package org.keycloak.broker.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.userprofile.AttributeChangeListener;
import org.keycloak.userprofile.EventAuditingAttributeChangeListener;
import org.keycloak.utils.StringUtil;

/**
 * Tracks the changes made to a user while it is updated from a brokered identity (e.g. by the identity provider and its
 * mappers) and reports them like other profile updates: a changed email as {@link EventType#UPDATE_EMAIL} and any
 * other change as {@link EventType#UPDATE_PROFILE}, with {@code previous_*} and {@code updated_*} details.
 *
 * <p>The tracker is a {@link UserModelDelegate} that must be passed to the code updating the user, so that only the
 * attributes written through it are reported. Comparing all attributes before and after the update would be simpler,
 * but would report changes made elsewhere as made by the identity provider: e.g. a user federated from LDAP may be read
 * from a stale cache before the update, while the first write replaces it with a user whose values are read from LDAP.
 *
 * <p>The value of a written attribute is read before its first write and compared with the value read after the
 * update, so a value normalized by the storage (e.g. a lower-cased email) is only reported if the stored value
 * changed, and an attribute written and restored during the update is not reported. Blank values are ignored, like in
 * the user profile. If the user is read from a stale cache before the first write, the previous value is the cached
 * one, i.e. the value used by Keycloak until then.
 */
public final class BrokeredUserChangeTracker extends UserModelDelegate {

    /**
     * The value of the {@link Details#CONTEXT} detail of the events.
     */
    public static final String IDP_SYNC_CONTEXT = "IDP_SYNC";

    private final Map<String, List<String>> previousValues = new HashMap<>();

    private BrokeredUserChangeTracker(UserModel user) {
        super(user);
    }

    /**
     * Starts tracking the changes made to the given {@code user}.
     *
     * @param user the user about to be updated
     * @return the tracker to update the user with
     */
    public static BrokeredUserChangeTracker track(UserModel user) {
        return new BrokeredUserChangeTracker(user);
    }

    @Override
    public void setUsername(String username) {
        recordPreviousValue(USERNAME);
        super.setUsername(username);
    }

    @Override
    public void setEmail(String email) {
        recordPreviousValue(EMAIL);
        super.setEmail(email);
    }

    @Override
    public void setFirstName(String firstName) {
        recordPreviousValue(FIRST_NAME);
        super.setFirstName(firstName);
    }

    @Override
    public void setLastName(String lastName) {
        recordPreviousValue(LAST_NAME);
        super.setLastName(lastName);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        recordPreviousValue(name);
        super.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        recordPreviousValue(name);
        super.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        recordPreviousValue(name);
        super.removeAttribute(name);
    }

    /**
     * Sends the events for the changes made since tracking started, if any.
     *
     * @param event the event builder to clone the events from
     * @param context the brokered identity used to update the user
     */
    public void sendEvents(EventBuilder event, BrokeredIdentityContext context) {
        Map<String, List<String>> changes = getChanges();
        List<String> previousEmail = changes.remove(EMAIL);

        if (previousEmail != null) {
            EventBuilder updateEmail = createEvent(event, EventType.UPDATE_EMAIL, context);
            new EventAuditingAttributeChangeListener(updateEmail).onChange(EMAIL, this, previousEmail);
            updateEmail.success();
        }

        if (!changes.isEmpty()) {
            EventBuilder updateProfile = createEvent(event, EventType.UPDATE_PROFILE, context);
            AttributeChangeListener listener = new EventAuditingAttributeChangeListener(updateProfile);
            changes.forEach((name, previousValue) -> listener.onChange(name, this, previousValue));
            updateProfile.success();
        }
    }

    /**
     * @return the previous values of the attributes changed since tracking started
     */
    Map<String, List<String>> getChanges() {
        Map<String, List<String>> changes = new HashMap<>();
        previousValues.forEach((name, previousValue) -> {
            if (!CollectionUtil.collectionEquals(previousValue, readValue(name))) {
                changes.put(name, previousValue);
            }
        });
        return changes;
    }

    private void recordPreviousValue(String name) {
        previousValues.computeIfAbsent(name, this::readValue);
    }

    private List<String> readValue(String name) {
        return getAttributeStream(name).filter(StringUtil::isNotBlank).toList();
    }

    private EventBuilder createEvent(EventBuilder event, EventType type, BrokeredIdentityContext context) {
        return event.clone()
                .event(type)
                .user(this)
                .detail(Details.CONTEXT, IDP_SYNC_CONTEXT)
                .detail(Details.USERNAME, getUsername())
                .detail(Details.IDENTITY_PROVIDER, context.getIdpConfig().getAlias())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());
    }
}
