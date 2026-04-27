package org.keycloak.theme.freemarker;

import org.keycloak.provider.Provider;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;

public interface FreeMarkerProvider extends Provider {

    public String processTemplate(Object data, String templateName, Theme theme) throws FreeMarkerException;

}
