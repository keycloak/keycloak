package org.keycloak.wellknown;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface WellKnownProvider extends Provider {

    Object getConfig(RealmModel realm, UriInfo uriInfo);

}
