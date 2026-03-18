import { BaseEnvironment } from "@keycloak/keycloak-ui-shared";

export { PersonalInfo } from "./personal-info/PersonalInfo";
export { Header } from "./root/Header";
export { PageNav } from "./root/PageNav";
export { DeviceActivity } from "./account-security/DeviceActivity";
export { LinkedAccounts } from "./account-security/LinkedAccounts";
export { SigningIn } from "./account-security/SigningIn";
export type {
  AccountLinkUriRepresentation,
  Client,
  ClientRepresentation,
  ConsentRepresentation,
  ConsentScopeRepresentation,
  CredentialContainer,
  CredentialMetadataRepresentation,
  CredentialRepresentation,
  CredentialTypeMetadata,
  DeviceRepresentation,
  Group,
  LinkedAccountRepresentation,
  Permission,
  Permissions,
  Resource,
  Scope,
  SessionRepresentation,
  UserProfileAttributeMetadata,
  UserProfileMetadata,
  UserRepresentation,
} from "./api/representations";
export { Applications } from "./applications/Applications";
export { EmptyRow } from "./components/datalist/EmptyRow";
export { Page } from "./components/page/Page";
export { ContentComponent } from "./content/ContentComponent";
export { Groups } from "./groups/Groups";
export { EditTheResource } from "./resources/EditTheResource";
export { PermissionRequest } from "./resources/PermissionRequest";
export { Resources } from "./resources/Resources";
export { ResourcesTab } from "./resources/ResourcesTab";
export { ResourceToolbar } from "./resources/ResourceToolbar";
export { SharedWith } from "./resources/SharedWith";
export { Oid4Vci } from "./oid4vci/Oid4Vci";
export { Organizations } from "./organizations/Organizations";
export { ShareTheResource } from "./resources/ShareTheResource";
export {
  deleteConsent,
  deleteSession,
  getApplications,
  getCredentials,
  getDevices,
  getGroups,
  getLinkedAccounts,
  getPermissionRequests,
  getPersonalInfo,
  getSupportedLocales,
  savePersonalInfo,
  unLinkAccount,
} from "./api/methods";
export type AccountEnvironment = BaseEnvironment & {
  /** The URL to the root of the account console. */
  baseUrl: string;
  /** The locale of the user */
  locale: string;
  /** Name of the referrer application in the back link */
  referrerName?: string;
  /** UR to the referrer application in the back link */
  referrerUrl?: string;
  /** Feature flags */
  features: Feature;
};

export type Feature = {
  isRegistrationEmailAsUsername: boolean;
  isEditUserNameAllowed: boolean;
  isLinkedAccountsEnabled: boolean;
  isMyResourcesEnabled: boolean;
  deleteAccountAllowed: boolean;
  updateEmailFeatureEnabled: boolean;
  updateEmailActionEnabled: boolean;
  isViewGroupsEnabled: boolean;
  isViewOrganizationsEnabled: boolean;
  isOid4VciEnabled: boolean;
};

export { KeycloakProvider, useEnvironment } from "@keycloak/keycloak-ui-shared";
export { useAccountAlerts } from "./utils/useAccountAlerts";
export { usePromise } from "./utils/usePromise";
export { routes } from "./routes";
