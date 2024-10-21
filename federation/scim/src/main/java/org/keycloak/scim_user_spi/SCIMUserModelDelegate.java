package org.keycloak.scim_user_spi;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;

public class SCIMUserModelDelegate extends UserModelDelegate {

    private static final Logger logger = Logger.getLogger(SCIMUserModelDelegate.class);

    private ComponentModel model;

    private final Scim scim;

    public SCIMUserModelDelegate(Scim scim, UserModel delegate, ComponentModel model) {
        super(delegate);
        this.model = model;
        this.scim = scim;
    }

    @Override
    public void setAttribute(String attr, List<String> values) {
        SimpleHttp.Response resp = this.scim.updateUser(scim, this.getUsername(), attr, values);
        try {
            if (resp.getStatus() != HttpStatus.SC_OK && resp.getStatus() != HttpStatus.SC_NO_CONTENT) {
                logger.warn("Unexpected PUT status code returned");
                resp.close();
                return;
            }
            resp.close();
        } catch (IOException e) {
            logger.errorv("Error: {0}", e.getMessage());
            throw new RuntimeException(e);
        }
        super.setAttribute(attr, values);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        super.setSingleAttribute(name, value);
    }

    @Override
    public void setUsername(String username) {
        super.setUsername(username);
    }

    @Override
    public void setLastName(String lastName) {
        super.setLastName(lastName);
    }

    @Override
    public void setFirstName(String first) {
        super.setFirstName(first);
    }

    @Override
    public void setEmail(String email) {
        super.setFirstName(email);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }
}
