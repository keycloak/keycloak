package org.keycloak.scim.model.filter;

/**
 * Resolves SCIM attribute paths to Keycloak attribute names.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public interface AttributeNameResolver {

    AttributeInfo resolve(String scimAttrPath);
}
