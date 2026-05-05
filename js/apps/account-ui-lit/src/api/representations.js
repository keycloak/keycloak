/**
 * @typedef {'text' | 'textarea' | 'select' | 'select-radiobuttons' | 'multiselect' | 'multiselect-checkboxes' | 'html5-email' | 'html5-tel' | 'html5-url' | 'html5-number' | 'html5-range' | 'html5-datetime-local' | 'html5-date' | 'html5-month' | 'html5-time'} InputType
 */

/**
 * @typedef {Object} UserProfileAttributeMetadata
 * @property {string} name
 * @property {string} [displayName]
 * @property {boolean} [required]
 * @property {boolean} [readOnly]
 * @property {Record<string, Record<string, unknown>>} [validators]
 * @property {{inputType?: InputType, inputTypePlaceholder?: string, inputTypeCols?: number, inputTypeRows?: number, inputOptionLabels?: Record<string, string>, inputOptionLabelsI18nPrefix?: string}} [annotations]
 * @property {string} [group]
 * @property {boolean} [multivalued]
 * @property {string} [defaultValue]
 */

/**
 * @typedef {Object} UserProfileMetadata
 * @property {UserProfileAttributeMetadata[]} [attributes]
 * @property {{name: string, displayHeader?: string, displayDescription?: string}[]} [groups]
 */

/**
 * @typedef {Object} UserRepresentation
 * @property {string} [id]
 * @property {string} [username]
 * @property {string} [firstName]
 * @property {string} [lastName]
 * @property {string} [email]
 * @property {boolean} [emailVerified]
 * @property {Record<string, string | string[]>} [attributes]
 * @property {UserProfileMetadata} [userProfileMetadata]
 */

/**
 * @typedef {Object} SessionRepresentation
 * @property {string} id
 * @property {string} ipAddress
 * @property {number} started
 * @property {number} lastAccess
 * @property {number} expires
 * @property {string} browser
 * @property {boolean} current
 * @property {ClientRepresentation[]} clients
 */

/**
 * @typedef {Object} DeviceRepresentation
 * @property {string} id
 * @property {string} ipAddress
 * @property {string} os
 * @property {string} osVersion
 * @property {string} browser
 * @property {string} device
 * @property {number} lastAccess
 * @property {boolean} current
 * @property {boolean} mobile
 * @property {SessionRepresentation[]} sessions
 */

/**
 * @typedef {Object} ConsentScopeRepresentation
 * @property {string} id
 * @property {string} name
 * @property {string} displayText
 */

/**
 * @typedef {Object} ConsentRepresentation
 * @property {number} createdDate
 * @property {number} lastUpdatedDate
 * @property {ConsentScopeRepresentation[]} grantedScopes
 */

/**
 * @typedef {Object} ClientRepresentation
 * @property {string} clientId
 * @property {string} [clientName]
 * @property {string} [description]
 * @property {boolean} [userConsentRequired]
 * @property {boolean} [inUse]
 * @property {boolean} [offlineAccess]
 * @property {string} [rootUrl]
 * @property {string} [baseUrl]
 * @property {string} [effectiveUrl]
 * @property {ConsentRepresentation} [consent]
 * @property {string} [logoUri]
 * @property {string} [policyUri]
 * @property {string} [tosUri]
 */

/**
 * @typedef {Object} CredentialRepresentation
 * @property {string} id
 * @property {string} type
 * @property {string} [userLabel]
 * @property {number} createdDate
 * @property {string} [credentialData]
 */

/**
 * @typedef {Object} CredentialMetadataRepresentation
 * @property {CredentialRepresentation} credential
 */

/**
 * @typedef {Object} CredentialContainer
 * @property {string} type
 * @property {string} category
 * @property {string} displayName
 * @property {string} helptext
 * @property {string} iconCssClass
 * @property {string} createAction
 * @property {string} updateAction
 * @property {boolean} removeable
 * @property {CredentialMetadataRepresentation[]} userCredentialMetadatas
 */

/**
 * @typedef {Object} LinkedAccountRepresentation
 * @property {boolean} connected
 * @property {string} providerAlias
 * @property {string} providerName
 * @property {string} displayName
 * @property {string} linkedUsername
 */

/**
 * @typedef {Object} Group
 * @property {string} id
 * @property {string} name
 * @property {string} path
 */

/**
 * @typedef {Object} Scope
 * @property {string} id
 * @property {string} name
 * @property {string} [displayName]
 */

/**
 * @typedef {Object} Resource
 * @property {string} _id
 * @property {string} name
 * @property {string} [displayName]
 * @property {string} [type]
 * @property {{id: string, name: string}} owner
 * @property {boolean} ownerManagedAccess
 * @property {string[]} uris
 * @property {Scope[]} scopes
 */

/**
 * @typedef {Object} Permission
 * @property {string} username
 * @property {string[]} scopes
 */

/**
 * @typedef {Object} OrganizationRepresentation
 * @property {string} [id]
 * @property {string} [name]
 * @property {string} [alias]
 * @property {boolean} [enabled]
 * @property {string} [description]
 * @property {Record<string, string[]>} [attributes]
 * @property {{name: string, verified: boolean}[]} [domains]
 */

export {};
