package org.keycloak.models.cache.infinispan.stream.entities;

import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.cache.RealmCache;
import org.keycloak.models.cache.entities.CachedClientTemplate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RevisionedCachedClientTemplate extends CachedClientTemplate implements Revisioned, InRealm {

    public RevisionedCachedClientTemplate(Long revision, RealmCache cache, RealmProvider delegate, RealmModel realm, ClientTemplateModel model) {
        super(cache, delegate, realm, model);
        this.revision = revision;
    }

    private Long revision;

    @Override
    public Long getRevision() {
        return revision;
    }

    @Override
    public void setRevision(Long revision) {
        this.revision = revision;
    }


}
