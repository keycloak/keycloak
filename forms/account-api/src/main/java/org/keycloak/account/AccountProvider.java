package org.keycloak.account;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AccountProvider {

    public Account createAccount(UriInfo uriInfo);

}
