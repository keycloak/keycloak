package org.keycloak.models.mongo.api.context;

import org.keycloak.models.mongo.api.MongoStore;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface MongoTask {

    void execute();

    boolean isFullUpdate();
}
