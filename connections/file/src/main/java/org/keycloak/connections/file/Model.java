package org.keycloak.connections.file;

import org.keycloak.representations.idm.RealmRepresentation;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Model {
    private String modelVersion;
    private List<RealmRepresentation> realms;

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public List<RealmRepresentation> getRealms() {
        return realms;
    }

    public void setRealms(List<RealmRepresentation> realms) {
        this.realms = realms;
    }
}
