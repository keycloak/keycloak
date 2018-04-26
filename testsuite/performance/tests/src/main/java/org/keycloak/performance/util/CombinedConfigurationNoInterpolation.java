package org.keycloak.performance.util;

import org.apache.commons.configuration.CombinedConfiguration;

/**
 * CombinedConfigurationNoInterpolation. This class disables variable interpolation (substution)
 * because Freemarker which is used for entity templating uses the same syntax: ${property}.
 * 
 * @author tkyjovsk
 */
public class CombinedConfigurationNoInterpolation extends CombinedConfiguration {

    @Override
    protected Object interpolate(Object value) {
        return value;
    }

    @Override
    protected String interpolate(String base) {
        return base;
    }
    
}
