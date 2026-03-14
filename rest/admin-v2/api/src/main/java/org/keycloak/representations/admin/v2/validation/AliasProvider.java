package org.keycloak.representations.admin.v2.validation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseRepresentation;

/**
 * Used by {@link UuidUnmodifiedValidator} to get the persisted alias for a given UUID and to get the alias
 * from the type specific representation.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public interface AliasProvider {
    String getPersistedAlias(KeycloakSession session, RealmModel realm, String uuid);
    String getAliasFromRepresentation(BaseRepresentation representation);
}
