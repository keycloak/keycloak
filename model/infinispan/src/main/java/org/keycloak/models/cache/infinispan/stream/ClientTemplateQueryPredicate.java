package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.ClientTemplateQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplateQueryPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String template;

    public static ClientTemplateQueryPredicate create() {
        return new ClientTemplateQueryPredicate();
    }

    public ClientTemplateQueryPredicate template(String template) {
        this.template = template;
        return this;
    }





    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof ClientTemplateQuery)) return false;
        ClientTemplateQuery query = (ClientTemplateQuery)value;


        return query.getTemplates().contains(template);
    }
}
