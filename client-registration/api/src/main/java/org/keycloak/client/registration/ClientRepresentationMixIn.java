package org.keycloak.client.registration;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
abstract class ClientRepresentationMixIn {

    @JsonIgnore
    String registrationAccessToken;

}
