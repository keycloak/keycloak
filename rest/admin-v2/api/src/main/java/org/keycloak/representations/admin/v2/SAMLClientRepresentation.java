package org.keycloak.representations.admin.v2;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Schema
public class SAMLClientRepresentation extends BaseClientRepresentation {
    public static final String PROTOCOL = "saml";

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }
}
