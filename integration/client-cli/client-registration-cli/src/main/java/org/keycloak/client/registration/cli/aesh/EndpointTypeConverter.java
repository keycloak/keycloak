package org.keycloak.client.registration.cli.aesh;

import org.jboss.aesh.cl.converter.Converter;
import org.jboss.aesh.cl.validator.OptionValidatorException;
import org.jboss.aesh.console.command.converter.ConverterInvocation;
import org.keycloak.client.registration.cli.common.EndpointType;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class EndpointTypeConverter implements Converter<EndpointType, ConverterInvocation> {

    @Override
    public EndpointType convert(ConverterInvocation converterInvocation) throws OptionValidatorException {
        return EndpointType.of(converterInvocation.getInput());
    }
}
