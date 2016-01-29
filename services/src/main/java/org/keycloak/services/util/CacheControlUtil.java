package org.keycloak.services.util;

import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;

import javax.ws.rs.core.CacheControl;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CacheControlUtil {

    public static void noBackButtonCacheControlHeader() {
        HttpResponse response = ResteasyProviderFactory.getContextData(HttpResponse.class);
        response.getOutputHeaders().putSingle("Cache-Control", "no-store, must-revalidate, max-age=0");
    }

    public static CacheControl getDefaultCacheControl() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        Integer maxAge = Config.scope("theme").getInt("staticMaxAge");
        if (maxAge != null && maxAge > 0) {
            cacheControl.setMaxAge(maxAge);
        } else {
            cacheControl.setNoCache(true);
        }
        return cacheControl;
    }

    public static CacheControl noCache() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        return cacheControl;
    }

}
