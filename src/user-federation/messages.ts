export default {
  "user-federation": {
    userFederation: "User federation",
    descriptionLanding:
      "This is the description for the user federation landing page",
    userFederationExplain:
      "Keycloak can federate external user databases. Out of the box we have support for LDAP and Active Directory.",
    getStarted: "To get started, select a provider from the list below.",
    providers: "Add providers",
    addKerberos: "Add Kerberos providers",
    addLdap: "Add LDAP providers",
    addOneLdap: "Add LDAP provider",
    addKerberosWizardTitle: "Add Kerberos user federation provider",
    addLdapWizardTitle: "Add LDAP user federation provider",

    syncChangedUsers: "Sync changed users",
    syncAllUsers: "Sync all users",
    unlinkUsers: "Unlink users",
    removeImported: "Remove imported",
    deleteProvider: "Delete provider",

    generalOptions: "General options",
    consoleDisplayName: "Console display name",
    vendor: "Vendor",

    connectionAndAuthenticationSettings:
      "Connection and authentication settings",
    connectionURL: "Connection URL",
    enableStartTls: "Enable StartTLS",
    useTruststoreSpi: "Use Truststore SPI",
    connectionPooling: "Connection pooling",
    connectionTimeout: "Connection timeout",
    bindType: "Bind type",
    bindDn: "Bind DN",
    bindCredentials: "Bind credentials",

    ldapSearchingAndUpdatingSettings: "LDAP searching and updating",
    editMode: "Edit mode",
    usersDN: "Users DN",
    usernameLdapAttribute: "Username LDAP attribute",
    rdnLdapAttribute: "RDN LDAP attribute",
    uuidLdapAttribute: "UUID LDAP attribute",
    userObjectClasses: "User object classes",
    userLdapFilter: "User LDAP filter",
    searchScope: "Search scope",
    readTimeout: "Read timeout",
    pagination: "Pagination",

    synchronizationSettings: "Synchronization settings",
    importUsers: "Import users",
    batchSize: "Batch size",
    periodicFullSync: "Periodic full sync",
    fullSyncPeriod: "Full sync period",
    periodicChangedUsersSync: "Periodic changed users sync",
    changedUsersSyncPeriod: "Changed users sync period",

    kerberosIntegration: "Kerberos integration",
    allowKerberosAuthentication: "Allow Kerberos authentication",
    useKerberosForPasswordAuthentication:
      "Use Kerberos for password authentication",

    cacheSettings: "Cache settings",
    cachePolicy: "Cache policy",
    evictionDay: "Eviction day",
    evictionHour: "Eviction hour",
    evictionMinute: "Eviction minute",
    maxLifespan: "Max lifespan",

    advancedSettings: "Advanced settings",
    enableLdapv3Password:
      "Enable the LDAPv3 password modify extended operation",
    validatePasswordPolicy: "Validate password policy",
    trustEmail: "Trust email",

    requiredSettings: "Required Settings",
    kerberosRealm: "Kerberos realm",
    serverPrincipal: "Server principal",
    keyTab: "Key tab",
    debug: "Debug",
    allowPasswordAuthentication: "Allow password authentication",
    updateFirstLogin: "Update first login",

    always: "Always",
    never: "Never",
    onlyLdaps: "Only for ldaps",
    oneLevel: "One Level",
    subtree: "Subtree",

    saveSuccess: "User federation provider successfully saved",
    saveError: "User federation provider could not be saved: {{error}}",
    createSuccess: "User federation provider successfully created",
    createError: "User federation provider could not be created: {{error}}",
    testAuthentication: "Test authentication",
    testSuccess: "Successfully connected to LDAP",
    testError:
      "Error when trying to connect to LDAP. See server.log for details. {{error}}",

    learnMore: "Learn more",
    addNewProvider: "Add new provider",
    userFedDeletedSuccess: "The user federation provider has been deleted.",
    userFedDeleteError:
      "Could not delete user federation provider: '{{error}}'",
    userFedDeleteConfirmTitle: "Delete user federation provider?",
    userFedDeleteConfirm:
      "If you delete this user federation provider, all associated data will be removed.",
    userFedDisableConfirmTitle: "Disable user federation provider?",
    userFedDisableConfirm:
      "If you disable this user federation provider, it will not be considered for queries and imported users will be disabled and read-only until the provider is enabled again.",

    userFedUnlinkUsersConfirmTitle: "Unlink all users?",
    userFedUnlinkUsersConfirm:
      "Do you want to unlink all the users? Any users without a password in the database will not be able to authenticate anymore.",

    removeImportedUsers: "Remove imported users?",
    removeImportedUsersMessage:
      "Do you really want to remove all imported users?",
    removeImportedUsersSuccess: "Imported users have been removed.",
    removeImportedUsersError: "Could not remove imported users: '{{error}}'",

    syncUsersSuccess: "Sync of users finished successfully.",
    syncUsersError: "Could not sync users: '{{error}}'",

    unlinkUsersSuccess: "Unlink of users finished successfully.",
    unlinkUsersError: "Could not unlink users: '{{error}}'",

    validateName: "You must enter a name",
    validateRealm: "You must enter a realm",
    validateServerPrincipal: "You must enter a server principal",
    validateKeyTab: "You must enter a key tab",
    validateConnectionUrl: "You must enter a connection URL",
    validateBindDn: "You must enter the DN of the LDAP admin",
    validateBindCredentials: "You must enter the password of the LDAP admin",
    validateUuidLDAPAttribute: "You must enter a UUID LDAP attribute",
    validateUserObjectClasses: "You must enter one or more user object classes",
    validateEditMode: "You must select an edit mode",
    validateUsersDn: "You must enter users DN",
    validateUsernameLDAPAttribute: "You must enter a username LDAP attribute",
    validateRdnLdapAttribute: "You must enter an RDN LDAP attribute",
    validateCustomUserSearchFilter:
      "Filter must be enclosed in parentheses, for example: (filter)",

    mapperTypeMsadUserAccountControlManager: "msad-user-account-control-mapper",
    mapperTypeMsadLdsUserAccountControlMapper:
      "msad-user-account-control-mapper",
    mapperTypeGroupLdapMapper: "group-ldap-mapper",
    mapperTypeUserAttributeLdapMapper: "user-attribute-ldap-mapper",
    mapperTypeRoleLdapMapper: "role-ldap-mapper",
    mapperTypeHardcodedAttributeMapper: "hardcoded-attribute-mapper",
    mapperTypeHardcodedLdapRoleMapper: "hardcoded-ldap-role-mapper",
    mapperTypeCertificateLdapMapper: "certificate-ldap-mapper",
    mapperTypeFullNameLdapMapper: "full-name-ldap-mapper",
    mapperTypeHardcodedLdapGroupMapper: "hardcoded-ldap-group-mapper",
    mapperTypeLdapAttributeMapper: "hardcoded-ldap-attribute-mapper",

    ldapMappersList: "LDAP Mappers",

    ldapFullNameAttribute: "LDAP full name attribute",
    writeOnly: "Write only",

    ldapGroupsDn: "LDAP groups DN",
    groupNameLdapAttribute: "Group name LDAP attribute",
    groupObjectClasses: "Group object classes",
    preserveGroupInheritance: "Preserve group inheritance",
    ignoreMissingGroups: "Ignore missing groups",
    userGroupsRetrieveStrategy: "User groups retrieve strategy",
    mappedGroupAttributes: "Mapped group attributes",
    dropNonexistingGroupsDuringSync: "Drop non-existing groups during sync",
    groupsPath: "Groups path",

    membershipLdapAttribute: "Membership LDAP attribute",
    membershipAttributeType: "Membership attribute type",
    membershipUserLdapAttribute: "Membership user LDAP attribute",
    ldapFilter: "LDAP filter",
    mode: "Mode",
    memberofLdapAttribute: "Member-of LDAP attribute",

    ldapRolesDn: "LDAP roles DN",
    roleNameLdapAttribute: "Role name LDAP attribute",
    roleObjectClasses: "Role object classes",
    userRolesRetrieveStrategy: "User roles retrieve strategy",
    useRealmRolesMapping: "Use realm roles mapping",

    ldapAttributeName: "LDAP attribute name",
    ldapAttributeValue: "LDAP attribute value",

    userModelAttribute: "User model attribute",
    ldapAttribute: "LDAP attribute",
    readOnly: "Read only",
    alwaysReadValueFromLdap: "Always read value from LDAP",
    isMandatoryInLdap: "Is mandatory in LDAP",
    attributeDefaultValue: "Attribute default value",
    isBinaryAttribute: "Is binary attribute",
    derFormatted: "DER formatted",

    passwordPolicyHintsEnabled: "Password policy hints enabled",

    userModelAttributeName: "User model attribute name",
    attributeValue: "Attribute value",

    selectRole: "Select role",

    group: "Group",
  },
};
