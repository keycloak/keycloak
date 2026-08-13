package org.keycloak.protocol.oidc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;

public abstract class AbstractClientConfigWrapper {
    protected final ClientModel clientModel;
    protected final ClientRepresentation clientRep;

    protected AbstractClientConfigWrapper(ClientModel clientModel,
                                          ClientRepresentation clientRep) {
        this.clientModel = clientModel;
        this.clientRep = clientRep;
    }

    protected String getAttribute(String attrKey) {
        if (clientModel != null) {
            return clientModel.getAttribute(attrKey);
        } else {
            return clientRep.getAttributes() == null ? null : clientRep.getAttributes().get(attrKey);
        }
    }

    protected String getAttribute(String attrKey, String defaultValue) {
        String value = getAttribute(attrKey);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    protected Object getAttributes() {
        if (clientModel != null) return clientModel.getAttributes();
        else
            return clientRep.getAttributes();
    }

    protected void setAttribute(String attrKey, String attrValue) {
        if (clientModel != null) {
            if (attrValue != null) {
                clientModel.setAttribute(attrKey, attrValue);
            } else {
                clientModel.removeAttribute(attrKey);
            }
        } else {
            if (attrValue != null) {
                if (clientRep.getAttributes() == null) {
                    clientRep.setAttributes(new HashMap<>());
                }
                clientRep.getAttributes().put(attrKey, attrValue);
            } else {
                if (clientRep.getAttributes() != null) {
                    clientRep.getAttributes().put(attrKey, null);
                }
            }
        }
    }

    public List<String> getAttributeMultivalued(String attrKey) {
        String attrValue = getAttribute(attrKey);
        if (attrValue == null) return Collections.emptyList();
        return Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(attrValue));
    }

    public void setAttributeMultivalued(String attrKey, List<String> attrValues) {
        if (attrValues == null || attrValues.size() == 0) {
            // Remove attribute
            setAttribute(attrKey, null);
        } else {
            String attrValueFull = String.join(Constants.CFG_DELIMITER, attrValues);
            setAttribute(attrKey, attrValueFull);
        }
    }
}
