package org.keycloak.freemarker;

import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrowserSecurityHeaderSetup {

    public static Response.ResponseBuilder headers(Response.ResponseBuilder builder, RealmModel realm) {
        for (Map.Entry<String, String> entry : realm.getBrowserSecurityHeaders().entrySet()) {
            String headerName = BrowserSecurityHeaders.headerAttributeMap.get(entry.getKey());
            if (headerName == null) continue;
            builder.header(headerName, entry.getValue());
        }
        return builder;
    }
}
