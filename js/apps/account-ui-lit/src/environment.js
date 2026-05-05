/**
 * @typedef {Object} Feature
 * @property {boolean} isRegistrationEmailAsUsername
 * @property {boolean} isEditUserNameAllowed
 * @property {boolean} isLinkedAccountsEnabled
 * @property {boolean} isMyResourcesEnabled
 * @property {boolean} deleteAccountAllowed
 * @property {boolean} updateEmailFeatureEnabled
 * @property {boolean} updateEmailActionEnabled
 * @property {boolean} isViewGroupsEnabled
 * @property {boolean} isViewOrganizationsEnabled
 * @property {boolean} isOid4VciEnabled
 */

/**
 * @typedef {Object} Environment
 * @property {string} serverBaseUrl
 * @property {string} baseUrl
 * @property {string} realm
 * @property {string} clientId
 * @property {string} resourceUrl
 * @property {string} logo
 * @property {string} logoUrl
 * @property {string} locale
 * @property {string} [referrerName]
 * @property {string} [referrerUrl]
 * @property {Feature} features
 */

/** @type {Environment} */
const DEFAULT_ENVIRONMENT = {
  serverBaseUrl: "http://localhost:8080",
  baseUrl: "http://localhost:8080/realms/master/account/",
  realm: "master",
  clientId: "account-console",
  resourceUrl: "http://localhost:5174",
  logo: "logo.svg",
  logoUrl: "/",
  locale: "en",
  features: {
    isRegistrationEmailAsUsername: false,
    isEditUserNameAllowed: true,
    isLinkedAccountsEnabled: true,
    isMyResourcesEnabled: true,
    deleteAccountAllowed: true,
    updateEmailFeatureEnabled: true,
    updateEmailActionEnabled: true,
    isViewGroupsEnabled: true,
    isViewOrganizationsEnabled: true,
    isOid4VciEnabled: false,
  },
};

/**
 * @returns {Environment}
 */
export function getEnvironment() {
  const env = window.__env__ ?? DEFAULT_ENVIRONMENT;
  return {
    ...env,
    resourceUrl: env.resourceUrl ?? new URL(env.baseUrl).origin,
  };
}

export const environment = getEnvironment();
