package org.keycloak.client.registration;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
abstract class OIDCClientRepresentationMixIn {

    @JsonIgnore
    private Integer client_id_issued_at;

    @JsonIgnore
    private Integer client_secret_expires_at;

    @JsonIgnore
    private String registration_client_uri;

    @JsonIgnore
    private String registration_access_token;

}
