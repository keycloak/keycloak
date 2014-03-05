package org.keycloak;

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
public class SkeletonKeyContextResolver implements ContextResolver<ObjectMapper> {
    protected ObjectMapper mapper = new ObjectMapper();

    public SkeletonKeyContextResolver() {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public SkeletonKeyContextResolver(boolean indent) {
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        if (indent) {
            mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        }
    }


    @Override
    public ObjectMapper getContext(Class<?> type) {
        if (type.getPackage().getName().startsWith(getClass().getPackage().getName())) return mapper;
        return null;
    }
}
