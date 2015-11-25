package org.keycloak.adapters.spi;

/**
 * Common marker interface used by keycloak client adapters when there is an error.  For servlets, you'll be able
 * to extract this error from the HttpServletRequest.getAttribute(LogoutError.class.getName()).  Each protocol
 * will have their own subclass of this interface.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LogoutError {
}
