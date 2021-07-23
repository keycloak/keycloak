import type { TFunction } from "i18next";
import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";
import type { ComponentType } from "react";
import type { MatchOptions } from "use-react-router-breadcrumbs";
import authenticationRoutes from "./authentication/routes";
import clientScopesRoutes from "./client-scopes/routes";
import clientRoutes from "./clients/routes";
import { DashboardSection } from "./dashboard/Dashboard";
import { EventsSection } from "./events/EventsSection";
import { GroupsSection } from "./groups/GroupsSection";
import { SearchGroups } from "./groups/SearchGroups";
import {
  AddIdentityProvider,
  IdentityProviderCrumb,
} from "./identity-providers/add/AddIdentityProvider";
import { AddOpenIdConnect } from "./identity-providers/add/AddOpenIdConnect";
import { DetailSettings } from "./identity-providers/add/DetailSettings";
import { IdentityProvidersSection } from "./identity-providers/IdentityProvidersSection";
import { PageNotFoundSection } from "./PageNotFoundSection";
import realmRoleRoutes from "./realm-roles/routes";
import { AESGeneratedSettings } from "./realm-settings/key-providers/aes-generated/AESGeneratedForm";
import { ECDSAGeneratedSettings } from "./realm-settings/key-providers/ecdsa-generated/ECDSAGeneratedForm";
import { HMACGeneratedSettings } from "./realm-settings/key-providers/hmac-generated/HMACGeneratedForm";
import { JavaKeystoreSettings } from "./realm-settings/key-providers/java-keystore/JavaKeystoreForm";
import { RSAGeneratedSettings } from "./realm-settings/key-providers/rsa-generated/RSAGeneratedForm";
import { RSASettings } from "./realm-settings/key-providers/rsa/RSAForm";
import {
  EditProviderCrumb,
  RealmSettingsSection,
} from "./realm-settings/RealmSettingsSection";
import realmRoutes from "./realm/routes";
import sessionRoutes from "./sessions/routes";
import { LdapMapperDetails } from "./user-federation/ldap/mappers/LdapMapperDetails";
import { UserFederationKerberosSettings } from "./user-federation/UserFederationKerberosSettings";
import { UserFederationLdapSettings } from "./user-federation/UserFederationLdapSettings";
import { UserFederationSection } from "./user-federation/UserFederationSection";
import userRoutes from "./user/routes";

export type RouteDef = {
  path: string;
  component: ComponentType;
  breadcrumb?: (t: TFunction) => string | ComponentType<any>;
  access: AccessType;
  matchOptions?: MatchOptions;
};

export const routes: RouteDef[] = [
  ...authenticationRoutes,
  ...clientRoutes,
  ...clientScopesRoutes,
  ...realmRoleRoutes,
  ...realmRoutes,
  ...sessionRoutes,
  ...userRoutes,
  {
    path: "/:realm/events/:tab?",
    component: EventsSection,
    breadcrumb: (t) => t("events:title"),
    access: "view-events",
  },
  {
    path: "/:realm/realm-settings/:tab?",
    component: RealmSettingsSection,
    breadcrumb: (t) => t("realmSettings"),
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/aes-generated/settings",
    component: AESGeneratedSettings,
    breadcrumb: () => EditProviderCrumb,
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/ecdsa-generated/settings",
    component: ECDSAGeneratedSettings,
    breadcrumb: () => EditProviderCrumb,
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/hmac-generated/settings",
    component: HMACGeneratedSettings,
    breadcrumb: () => EditProviderCrumb,
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/java-keystore/settings",
    component: JavaKeystoreSettings,
    breadcrumb: () => EditProviderCrumb,
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/rsa-generated/settings",
    component: RSAGeneratedSettings,
    breadcrumb: () => EditProviderCrumb,
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/rsa/settings",
    component: RSASettings,
    breadcrumb: () => EditProviderCrumb,
    access: "view-realm",
  },
  {
    path: "/:realm/identity-providers",
    component: IdentityProvidersSection,
    breadcrumb: (t) => t("identityProviders"),
    access: "view-identity-providers",
  },
  {
    path: "/:realm/identity-providers/oidc",
    component: AddOpenIdConnect,
    breadcrumb: (t) => t("identity-providers:addOpenIdProvider"),
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/identity-providers/keycloak-oidc",
    component: AddOpenIdConnect,
    breadcrumb: (t) => t("identity-providers:addKeycloakOpenIdProvider"),
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/identity-providers/:id",
    component: AddIdentityProvider,
    breadcrumb: () => IdentityProviderCrumb,
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/identity-providers/:id/:tab?",
    component: DetailSettings,
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/user-federation",
    component: UserFederationSection,
    breadcrumb: (t) => t("userFederation"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/kerberos",
    component: UserFederationSection,
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/kerberos/:id",
    component: UserFederationKerberosSettings,
    breadcrumb: (t) => t("common:settings"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/kerberos/new",
    component: UserFederationKerberosSettings,
    breadcrumb: (t) => t("common:settings"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap",
    component: UserFederationSection,
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap/new",
    component: UserFederationLdapSettings,
    breadcrumb: (t) => t("user-federation:addOneLdap"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap/:id/:tab?",
    component: UserFederationLdapSettings,
    breadcrumb: (t) => t("common:settings"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap/:id/:tab/:mapperId",
    component: LdapMapperDetails,
    breadcrumb: (t) => t("common:mappingDetails"),
    access: "view-realm",
  },
  {
    path: "/:realm/",
    component: DashboardSection,
    breadcrumb: (t) => t("common:home"),
    access: "anyone",
  },
  {
    path: "/:realm/groups/search",
    component: SearchGroups,
    breadcrumb: (t) => t("groups:searchGroups"),
    access: "query-groups",
  },
  {
    path: "/:realm/groups",
    component: GroupsSection,
    access: "query-groups",
    matchOptions: {
      exact: false,
    },
  },
  {
    path: "/",
    component: DashboardSection,
    breadcrumb: (t) => t("common:home"),
    access: "anyone",
  },
  {
    path: "*",
    component: PageNotFoundSection,
    access: "anyone",
  },
];
