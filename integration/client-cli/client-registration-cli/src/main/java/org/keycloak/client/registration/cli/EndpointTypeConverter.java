package org.keycloak.client.registration.cli;

import picocli.CommandLine.ITypeConverter;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class EndpointTypeConverter implements ITypeConverter<EndpointType> {

    @Override
    public EndpointType convert(String value) throws Exception {
        return EndpointType.of(value);
    }

}
