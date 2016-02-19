package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.stream.entities.InRealm;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientTemplateQuery extends InRealm {
    Set<String> getTemplates();
}
