export default {
  "client-scopes": {
    createClientScope: "Create client scope",
    clientScopeList: "Client scopes",
    grantedClientScopes: "Granted client scopes",
    clientScopeDetails: "Client scope details",
    clientScopeExplain:
      "Client scopes allow you to define a common set of protocol mappers and roles, which are shared between multiple clients",
    searchFor: "Search for client scope",
    protocol: "Protocol",
    assignedType: "Assigned type",
    displayOrder: "Display order",
    type: "Type",
    deleteClientScope: "Delete client scope {{name}}",
    deleteClientScope_plural: "Delete {{count}} client scopes",
    deleteConfirm: "Are you sure you want to delete this client scope",
    changeTypeTo: "Change type to",
    changeTypeIntro: "{{count}} selected client scopes will be changed to",
    clientScopeSuccess: "Scope mapping updated",
    clientScopeError: "Could not update scope mapping {{error}}",
    deletedSuccess: "The client scope has been deleted",
    deleteError: "Could not delete client scope: {{error}}",
    includeInTokenScope: "Include in token scope",
    realmRolePrefix: "Realm role prefix",
    userInfo: "User info",
    createSuccess: "Client scope created",
    createError: "Could not create client scope: '{{error}}'",
    updateSuccess: "Client scope updated",
    updateError: "Could not update client scope: '{{error}}'",
    addMapperExplain:
      "If you want more fine-grain control, you can create protocol mapper on this client",
    realmRoles: "Realm roles",
    selectARole: "Select a role",
    clientRoles: "Client roles",
    selectASourceOfRoles: "Select a source of roles",
    newRoleName: "New role name",
    searchClientByName: "Search client by name",
    clients: "Clients",
    mapperCreateSuccess: "New mapping has been added",
    mapperCreateError: "Could not create mapping: {{error}}",
    fromPredefinedMapper: "From predefined mappers",
    byConfiguration: "By configuration",
    emptyBuiltInMappersInstructions:
      "All built in mappers were added to this client",
    emptySecondaryAction: "Configure a new mapper",
    displayOnConsentScreen: "Display on consent screen",
    consentScreenText: "Consent screen text",
    guiOrder: "Display Order",
    shouldBeANumber: "Should be a number",
    chooseAMapperType: "Choose a mapper type",
    predefinedMappingDescription:
      "Choose one of the predefined mappings from this table",
    mappingTable: "Table with predefined mapping",
    roleGroup: "Use a realm role from:",
    clientGroup: "Use a client role from:",
    scope: "Scope",
    roleMappingUpdatedSuccess: "Role mapping updated",
    roleMappingUpdatedError: "Could not update role mapping {{error}}",
    protocolTypes: {
      all: "All",
      saml: "SAML",
      "openid-connect": "openid-connect",
    },

    usermodel: {
      prop: {
        label: "Property",
        tooltip:
          "Name of the property method in the UserModel interface. For example, a value of 'email' would reference the UserModel.getEmail() method.",
      },
      attr: {
        label: "User Attribute",
        tooltip:
          "Name of stored user attribute which is the name of an attribute within the UserModel.attribute map.",
      },
      clientRoleMapping: {
        client: {
          label: "Client ID",
          tooltip:
            "Client ID for role mappings. Just client roles of this client will be added to the token. If this is unset, client roles of all clients will be added to the token.",
        },
        rolePrefix: {
          label: "Client Role prefix",
          tooltip: "A prefix for each client role (optional).",
        },
        tokenClaimName: {
          tooltip:
            "Name of the claim to insert into the token. This can be a fully qualified name like 'address.street'. In this case, a nested json object will be created. To prevent nesting and use dot literally, escape the dot with backslash (\\.). The special token ${client_id} can be used and this will be replaced by the actual client ID. Example usage is 'resource_access.${client_id}.roles'. This is useful especially when you are adding roles from all the clients (Hence 'Client ID' switch is unset) and you want client roles of each client stored separately.",
        },
      },
      realmRoleMapping: {
        rolePrefix: {
          label: "Realm Role prefix",
          tooltip: "A prefix for each Realm Role (optional).",
        },
      },
    },
    userSession: {
      modelNote: {
        label: "User Session Note",
        tooltip:
          "Name of stored user session note within the UserSessionModel.note map.",
      },
    },
    multivalued: {
      label: "Multivalued",
      tooltip:
        "Indicates if attribute supports multiple values. If true, the list of all values of this attribute will be set as claim. If false, just first value will be set as claim",
    },
    aggregate: {
      attrs: {
        label: "Aggregate attribute values",
        tooltip:
          "Indicates if attribute values should be aggregated with the group attributes. If using OpenID Connect mapper the multivalued option needs to be enabled too in order to get all the values. Duplicated values are discarded and the order of values is not guaranteed with this option.",
      },
    },
    selectRole: {
      label: "Select Role",
      tooltip:
        "Enter role in the textbox to the left, or click this button to browse and select the role you want.",
    },
    tokenClaimName: {
      label: "Token Claim Name",
      tooltip:
        "Name of the claim to insert into the token. This can be a fully qualified name like 'address.street'. In this case, a nested json object will be created. To prevent nesting and use dot literally, escape the dot with backslash (\\.).",
    },
    jsonType: {
      label: "Claim JSON Type",
      tooltip:
        "JSON type that should be used to populate the json claim in the token. long, int, boolean, String and JSON are valid values.",
    },
    includeInIdToken: {
      label: "Add to ID token",
      tooltip: "Should the claim be added to the ID token?",
    },
    includeInAccessToken: {
      label: "Add to access token",
      tooltip: "Should the claim be added to the access token?",
    },
    includeInUserInfo: {
      label: "Add to userinfo",
      tooltip: "Should the claim be added to the userinfo?",
    },
    sectorIdentifierUri: {
      label: "Sector Identifier URI",
      tooltip:
        "Providers that use pairwise sub values and support Dynamic Client Registration SHOULD use the sector_identifier_uri parameter. It provides a way for a group of websites under common administrative control to have consistent pairwise sub values independent of the individual domain names. It also provides a way for Clients to change redirect_uri domains without having to reregister all their users.",
    },
    pairwiseSubAlgorithmSalt: {
      label: "Salt",
      tooltip:
        "Salt used when calculating the pairwise subject identifier. If left blank, a salt will be generated.",
    },
    addressClaim: {
      street: {
        label: "User Attribute Name for Street",
        tooltip:
          "Name of User Attribute, which will be used to map to 'street_address' subclaim inside 'address' token claim. Defaults to 'street' .",
      },
      locality: {
        label: "User Attribute Name for Locality",
        tooltip:
          "Name of User Attribute, which will be used to map to 'locality' subclaim inside 'address' token claim. Defaults to 'locality' .",
      },
      region: {
        label: "User Attribute Name for Region",
        tooltip:
          "Name of User Attribute, which will be used to map to 'region' subclaim inside 'address' token claim. Defaults to 'region' .",
      },
      postal_code: {
        label: "User Attribute Name for Postal Code",
        tooltip:
          "Name of User Attribute, which will be used to map to 'postal_code' subclaim inside 'address' token claim. Defaults to 'postal_code' .",
      },
      country: {
        label: "User Attribute Name for Country",
        tooltip:
          "Name of User Attribute, which will be used to map to 'country' subclaim inside 'address' token claim. Defaults to 'country' .",
      },
      formatted: {
        label: "User Attribute Name for Formatted Address",
        tooltip:
          "Name of User Attribute, which will be used to map to 'formatted' subclaim inside 'address' token claim. Defaults to 'formatted' .",
      },
    },
    included: {
      client: {
        audience: {
          label: "Included Client Audience",
          tooltip:
            "The Client ID of the specified audience client will be included in audience (aud) field of the token. If there are existing audiences in the token, the specified value is just added to them. It won't override existing audiences.",
        },
      },
      custom: {
        audience: {
          label: "Included Custom Audience",
          tooltip:
            "This is used just if 'Included Client Audience' is not filled. The specified value will be included in audience (aud) field of the token. If there are existing audiences in the token, the specified value is just added to them. It won't override existing audiences.",
        },
      },
    },
  },
};
