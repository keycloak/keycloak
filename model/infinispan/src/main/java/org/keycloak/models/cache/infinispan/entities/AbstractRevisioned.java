package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.common.util.Time;
import org.keycloak.models.cache.CachedObject;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractRevisioned implements Revisioned, CachedObject {
    private final String id;
    private long revision;
    private final long cacheTimestamp = Time.currentTimeMillis();

    public AbstractRevisioned(long revision, String id) {
        this.revision = revision;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getRevision() {
        return revision;
    }

    @Override
    public void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * When was this cached
     *
     * @return
     */
    @Override
    public long getCacheTimestamp() {
        return cacheTimestamp;
    }
}
