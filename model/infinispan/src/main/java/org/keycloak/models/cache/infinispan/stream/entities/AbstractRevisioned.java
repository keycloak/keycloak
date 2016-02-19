package org.keycloak.models.cache.infinispan.stream.entities;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractRevisioned implements Revisioned {
    private String id;
    private Long revision;

    public AbstractRevisioned(Long revision, String id) {
        this.revision = revision;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Long getRevision() {
        return revision;
    }

    @Override
    public void setRevision(Long revision) {
        this.revision = revision;
    }

}
