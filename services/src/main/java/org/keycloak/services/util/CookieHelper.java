package org.keycloak.services.util;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieHelper {

    /**
     * Set a response cookie.  This solely exists because JAX-RS 1.1 does not support setting HttpOnly cookies
     *
     * @param name
     * @param value
     * @param path
     * @param domain
     * @param comment
     * @param maxAge
     * @param secure
     * @param httpOnly
     */
    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly) {
        HttpServletResponse response = ResteasyProviderFactory.getContextData(HttpServletResponse.class);
        Cookie cookie = new Cookie(name, value);
        if (path != null) cookie.setPath(path);
        if (domain != null) cookie.setDomain(domain);
        if (comment != null) cookie.setComment(comment);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setHttpOnly(httpOnly);

        response.addCookie(cookie);

    }
}
