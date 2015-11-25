package org.keycloak.services.util;

import org.keycloak.Config;

import javax.ws.rs.core.CacheControl;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class CacheControlUtil {

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

}
