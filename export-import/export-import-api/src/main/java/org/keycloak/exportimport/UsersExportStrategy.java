package org.keycloak.exportimport;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum UsersExportStrategy {
    SKIP,            // Exporting of users will be skipped completely
    REALM_FILE,      // All users will be exported to same file with realm (So file like "foo-realm.json" with both realm data and users)
    SAME_FILE,       // All users will be exported to same file but different than realm (So file like "foo-realm.json" with realm data and "foo-users.json" with users)
    DIFFERENT_FILES  // Users will be exported into more different files according to maximum number of users per file
}
