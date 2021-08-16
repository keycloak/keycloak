package org.keycloak.userprofile;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

import java.util.List;

/**
 * A default listener implementation setting "attributesUpdatedTimestamp" in case of any attribute changes.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class DefaultAttributeChangeListener implements AttributeChangeListener {

    private static final String ATTRIBUTES_UPDATED_TIMESTAMP_SET = "org.keycloak.user.attributesUpdatedTimestampSet";

    private final KeycloakSession session;

    public DefaultAttributeChangeListener(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onChange(String name, UserModel user, List<String> oldValue) {
        setAttributesUpdatedTimestamp(user);
    }

    private void setAttributesUpdatedTimestamp(UserModel userModel) {
        if (!session.getAttributeOrDefault(ATTRIBUTES_UPDATED_TIMESTAMP_SET, Boolean.FALSE)) {
            userModel.setAttributesUpdatedTimestamp(Time.currentTimeMillis());
            session.setAttribute(ATTRIBUTES_UPDATED_TIMESTAMP_SET, Boolean.TRUE);
        }
    }

}
