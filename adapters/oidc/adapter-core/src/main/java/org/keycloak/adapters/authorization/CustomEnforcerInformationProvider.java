package org.keycloak.adapters.authorization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;

public interface CustomEnforcerInformationProvider {
    void authorize(HttpServletRequest request, HttpServletResponse response, Map<String, Set<String>> permissionMap);
}
