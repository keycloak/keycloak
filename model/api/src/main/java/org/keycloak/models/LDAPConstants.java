package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPConstants {

    public static final String LDAP_PROVIDER = "ldap";

    public static final String VENDOR = "vendor";
    public static final String VENDOR_RHDS = "rhds";
    public static final String VENDOR_ACTIVE_DIRECTORY = "ad";
    public static final String VENDOR_OTHER = "other";
    public static final String VENDOR_TIVOLI = "tivoli";
    public static final String VENDOR_NOVELL_EDIRECTORY="edirectory" ;

    public static final String USERNAME_LDAP_ATTRIBUTE = "usernameLDAPAttribute";
    public static final String RDN_LDAP_ATTRIBUTE = "rdnLDAPAttribute";
    public static final String UUID_LDAP_ATTRIBUTE = "uuidLDAPAttribute";
    public static final String USER_OBJECT_CLASSES = "userObjectClasses";

    public static final String CONNECTION_URL = "connectionUrl";
    public static final String SECURITY_PROTOCOL = "securityProtocol";
    public static final String BASE_DN = "baseDn"; // used for tests only
    public static final String USERS_DN = "usersDn";
    public static final String BIND_DN = "bindDn";
    public static final String BIND_CREDENTIAL = "bindCredential";

    public static final String AUTH_TYPE = "authType";
    public static final String AUTH_TYPE_NONE = "none";
    public static final String AUTH_TYPE_SIMPLE = "simple";

    public static final String SEARCH_SCOPE = "searchScope";
    public static final String CONNECTION_POOLING = "connectionPooling";
    public static final String PAGINATION = "pagination";

    public static final String EDIT_MODE = "editMode";

    // Count of users processed per single transaction during sync process
    public static final String BATCH_SIZE_FOR_SYNC = "batchSizeForSync";
    public static final int DEFAULT_BATCH_SIZE_FOR_SYNC = 1000;

    // Config option to specify if registrations will be synced or not
    public static final String SYNC_REGISTRATIONS = "syncRegistrations";

    // Applicable just for active directory
    public static final String USER_ACCOUNT_CONTROLS_AFTER_PASSWORD_UPDATE = "userAccountControlsAfterPasswordUpdate";

    // Custom attributes on UserModel, which is mapped to LDAP
    public static final String LDAP_ID = "LDAP_ID";
    public static final String LDAP_ENTRY_DN = "LDAP_ENTRY_DN";

    // String used in config to divide more possible values (for example more userDns), which are saved in DB as single string
    public static final String CONFIG_DIVIDER = ":::";

    // Those are forked from Picketlink
    public static final String GIVENNAME = "givenName";
    public static final String CN = "cn";
    public static final String SN = "sn";
    public static final String SAM_ACCOUNT_NAME = "sAMAccountName";
    public static final String EMAIL = "mail";
    public static final String POSTAL_CODE = "postalCode";
    public static final String STREET = "street";
    public static final String MEMBER = "member";
    public static final String MEMBER_OF = "memberOf";
    public static final String OBJECT_CLASS = "objectclass";
    public static final String UID = "uid";
    public static final String USER_PASSWORD_ATTRIBUTE = "userpassword";
    public static final String GROUP = "group";
    public static final String GROUP_OF_NAMES = "groupOfNames";
    public static final String GROUP_OF_ENTRIES = "groupOfEntries";
    public static final String GROUP_OF_UNIQUE_NAMES = "groupOfUniqueNames";

    public static final String COMMA = ",";
    public static final String EQUAL = "=";
    public static final String EMPTY_ATTRIBUTE_VALUE = " ";
    public static final String EMPTY_MEMBER_ATTRIBUTE_VALUE = "cn=empty-membership-placeholder";

    public static final String CUSTOM_ATTRIBUTE_ENABLED = "enabled";
    public static final String CUSTOM_ATTRIBUTE_CREATE_DATE = "createDate";
    public static final String CUSTOM_ATTRIBUTE_EXPIRY_DATE = "expiryDate";
    public static final String ENTRY_UUID = "entryUUID";
    public static final String OBJECT_GUID = "objectGUID";
    public static final String CREATE_TIMESTAMP = "createTimestamp";
    public static final String MODIFY_TIMESTAMP = "modifyTimestamp";

    public static String getUuidAttributeName(String vendor) {
        if (vendor != null) {
            switch (vendor) {
                case VENDOR_RHDS:
                    return "nsuniqueid";
                case VENDOR_TIVOLI:
                    return "uniqueidentifier";
                case VENDOR_NOVELL_EDIRECTORY:
                    return "guid";
                case VENDOR_ACTIVE_DIRECTORY:
                    return OBJECT_GUID;
            }
        }

        return ENTRY_UUID;
    }
}
