package org.keycloak.models.utils;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * Helper interface used just because RealmManager is in keycloak-services and not accessible for ImportUtils
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface RealmImporter {

    RealmModel importRealm(RealmRepresentation rep);
}
