package org.keycloak.client.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
abstract class ClientRepresentationMixIn {

    @JsonIgnore
    String registrationAccessToken;

}
