package org.keycloak.services.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * Any class with package org.jboss.resteasy.skeleton.key will use NON_DEFAULT inclusion
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Provider
public class ObjectMapperResolver implements ContextResolver<ObjectMapper> {
    protected ObjectMapper mapper = new ObjectMapper();

    public ObjectMapperResolver(boolean indent) {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        if (indent) {
            mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        }
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
