package org.keycloak.theme.freemarker;

import org.keycloak.provider.Provider;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;

import java.util.Map;

public interface FreeMarkerProvider extends Provider {

    public String processTemplate(Map<String, Object> attributes, String templateName, Theme theme) throws FreeMarkerException;

}
