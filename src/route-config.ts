import type { TFunction } from "i18next";
import type { BreadcrumbsRoute } from "use-react-router-breadcrumbs";
import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";

import { AuthenticationSection } from "./authentication/AuthenticationSection";
import { ClientScopeForm } from "./client-scopes/form/ClientScopeForm";
import { ClientScopesSection } from "./client-scopes/ClientScopesSection";
import { DashboardSection } from "./dashboard/Dashboard";
import { NewClientForm } from "./clients/add/NewClientForm";
import { ClientsSection } from "./clients/ClientsSection";
import { ImportForm } from "./clients/import/ImportForm";
import { EventsSection } from "./events/EventsSection";
import { GroupsSection } from "./groups/GroupsSection";
import { IdentityProvidersSection } from "./identity-providers/IdentityProvidersSection";
import { PageNotFoundSection } from "./PageNotFoundSection";
import { RealmRolesSection } from "./realm-roles/RealmRolesSection";
import { RealmSettingsSection } from "./realm-settings/RealmSettingsSection";
import { NewRealmForm } from "./realm/add/NewRealmForm";
import { SessionsSection } from "./sessions/SessionsSection";
import { UserFederationSection } from "./user-federation/UserFederationSection";
import { UsersSection } from "./user/UsersSection";
import { MappingDetails } from "./client-scopes/details/MappingDetails";
import { ClientDetails } from "./clients/ClientDetails";
import { UsersTabs } from "./user/UsersTabs";
import { UserGroups } from "./user/UserGroups";
import { UserFederationKerberosSettings } from "./user-federation/UserFederationKerberosSettings";
import { UserFederationLdapSettings } from "./user-federation/UserFederationLdapSettings";
import { RoleMappingForm } from "./client-scopes/add/RoleMappingForm";
import { RealmRoleTabs } from "./realm-roles/RealmRoleTabs";
import { SearchGroups } from "./groups/SearchGroups";
import { CreateInitialAccessToken } from "./clients/initial-access/CreateInitialAccessToken";
import { LdapMapperDetails } from "./user-federation/ldap/mappers/LdapMapperDetails";
import {
  AddIdentityProvider,
  IdentityProviderCrumb,
} from "./identity-providers/add/AddIdentityProvider";
import { AddOpenIdConnect } from "./identity-providers/add/AddOpenIdConnect";
import { DetailSettings } from "./identity-providers/add/DetailSettings";
import { AESGeneratedSettings } from "./realm-settings/key-providers/aes-generated/AESGeneratedForm";
import { HMACGeneratedSettings } from "./realm-settings/key-providers/hmac-generated/HMACGeneratedForm";
import { ECDSAGeneratedSettings } from "./realm-settings/key-providers/ecdsa-generated/ECDSAGeneratedForm";

export type RouteDef = BreadcrumbsRoute & {
  access: AccessType;
  component: () => JSX.Element;
};

type RoutesFn = (t: TFunction) => RouteDef[];

export const routes: RoutesFn = (t: TFunction) => [
  {
    path: "/:realm/add-realm",
    component: NewRealmForm,
    breadcrumb: t("realm:createRealm"),
    access: "manage-realm",
  },
  {
    path: "/:realm/clients/add-client",
    component: NewClientForm,
    breadcrumb: t("clients:createClient"),
    access: "manage-clients",
  },
  {
    path: "/:realm/clients/import-client",
    component: ImportForm,
    breadcrumb: t("clients:importClient"),
    access: "manage-clients",
  },
  {
    path: "/:realm/clients/:tab?",
    component: ClientsSection,
    breadcrumb: t("clients:clientList"),
    access: "query-clients",
  },
  {
    path: "/:realm/clients/initialAccessToken/create",
    component: CreateInitialAccessToken,
    breadcrumb: t("clients:createToken"),
    access: "manage-clients",
  },
  {
    path: "/:realm/clients/:clientId/roles/add-role",
    component: RealmRoleTabs,
    breadcrumb: t("roles:createRole"),
    access: "manage-realm",
  },
  {
    path: "/:realm/clients/:clientId/roles/:id/:tab?",
    component: RealmRoleTabs,
    breadcrumb: t("roles:roleDetails"),
    access: "view-realm",
  },
  {
    path: "/:realm/clients/:clientId/:tab",
    component: ClientDetails,
    breadcrumb: t("clients:clientSettings"),
    access: "view-clients",
  },
  {
    path: "/:realm/client-scopes/new",
    component: ClientScopeForm,
    breadcrumb: t("client-scopes:createClientScope"),
    access: "manage-clients",
  },
  {
    path: "/:realm/client-scopes/:id/mappers/oidc-role-name-mapper",
    component: RoleMappingForm,
    breadcrumb: t("common:mappingDetails"),
    access: "view-clients",
  },
  {
    path: "/:realm/client-scopes/:id/mappers/:mapperId",
    component: MappingDetails,
    breadcrumb: t("common:mappingDetails"),
    access: "view-clients",
  },
  {
    path: "/:realm/client-scopes/:id/:type/:tab",
    component: ClientScopeForm,
    breadcrumb: t("client-scopes:clientScopeDetails"),
    access: "view-clients",
  },
  {
    path: "/:realm/client-scopes",
    component: ClientScopesSection,
    breadcrumb: t("client-scopes:clientScopeList"),
    access: "view-clients",
  },
  {
    path: "/:realm/roles",
    component: RealmRolesSection,
    breadcrumb: t("roles:roleList"),
    access: "view-realm",
  },
  {
    path: "/:realm/roles/add-role",
    component: RealmRoleTabs,
    breadcrumb: t("roles:createRole"),
    access: "manage-realm",
  },
  {
    path: "/:realm/roles/:id/:tab?",
    component: RealmRoleTabs,
    breadcrumb: t("roles:roleDetails"),
    access: "view-realm",
  },
  {
    path: "/:realm/users",
    component: UsersSection,
    breadcrumb: t("users:title"),
    access: "query-users",
  },
  {
    path: "/:realm/users/add-user",
    component: UsersTabs,
    breadcrumb: t("users:createUser"),
    access: "manage-users",
  },
  {
    path: "/:realm/users/:id",
    component: UserGroups,
    breadcrumb: t("users:userDetails"),
    access: "manage-users",
  },
  {
    path: "/:realm/users/:id/:tab",
    component: UsersTabs,
    breadcrumb: t("users:userDetails"),
    access: "manage-users",
  },
  {
    path: "/:realm/sessions",
    component: SessionsSection,
    breadcrumb: t("sessions:title"),
    access: "view-realm",
  },
  {
    path: "/:realm/events/:tab?",
    component: EventsSection,
    breadcrumb: t("events:title"),
    access: "view-events",
  },
  {
    path: "/:realm/realm-settings/:tab?",
    component: RealmSettingsSection,
    breadcrumb: t("realmSettings"),
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/aes-generated/settings",
    component: AESGeneratedSettings,
    breadcrumb: t("realm-settings:editProvider"),
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/ecdsa-generated/settings",
    component: ECDSAGeneratedSettings,
    breadcrumb: t("realm-settings:editProvider"),
    access: "view-realm",
  },
  {
    path: "/:realm/realm-settings/keys/:id?/hmac-generated/settings",
    component: HMACGeneratedSettings,
    breadcrumb: t("realm-settings:editProvider"),
    access: "view-realm",
  },
  {
    path: "/:realm/authentication",
    component: AuthenticationSection,
    breadcrumb: t("authentication"),
    access: "view-realm",
  },
  {
    path: "/:realm/identity-providers",
    component: IdentityProvidersSection,
    breadcrumb: t("identityProviders"),
    access: "view-identity-providers",
  },
  {
    path: "/:realm/identity-providers/oidc",
    component: AddOpenIdConnect,
    breadcrumb: t("identity-providers:addOpenIdProvider"),
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/identity-providers/keycloak-oidc",
    component: AddOpenIdConnect,
    breadcrumb: t("identity-providers:addKeycloakOpenIdProvider"),
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/identity-providers/:id",
    component: AddIdentityProvider,
    breadcrumb: IdentityProviderCrumb,
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/identity-providers/:id/:tab?",
    component: DetailSettings,
    breadcrumb: null,
    access: "manage-identity-providers",
  },
  {
    path: "/:realm/user-federation",
    component: UserFederationSection,
    breadcrumb: t("userFederation"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/kerberos",
    component: UserFederationSection,
    breadcrumb: null,
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/kerberos/:id",
    component: UserFederationKerberosSettings,
    breadcrumb: t("common:settings"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/kerberos/new",
    component: UserFederationKerberosSettings,
    breadcrumb: t("common:settings"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap",
    component: UserFederationSection,
    breadcrumb: null,
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap/new",
    component: UserFederationLdapSettings,
    breadcrumb: t("user-federation:addOneLdap"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap/:id/:tab?",
    component: UserFederationLdapSettings,
    breadcrumb: t("common:settings"),
    access: "view-realm",
  },
  {
    path: "/:realm/user-federation/ldap/:id/:tab/:mapperId",
    component: LdapMapperDetails,
    breadcrumb: t("common:mappingDetails"),
    access: "view-realm",
  },
  {
    path: "/:realm/",
    component: DashboardSection,
    breadcrumb: t("common:home"),
    access: "anyone",
  },
  {
    path: "/:realm/groups/search",
    component: SearchGroups,
    breadcrumb: t("groups:searchGroups"),
    access: "query-groups",
  },
  {
    path: "/:realm/groups",
    component: GroupsSection,
    breadcrumb: null,
    matchOptions: {
      exact: false,
    },
    access: "query-groups",
  },
  {
    path: "/",
    component: DashboardSection,
    breadcrumb: t("common:home"),
    access: "anyone",
  },
  {
    path: "*",
    component: PageNotFoundSection,
    breadcrumb: null,
    access: "anyone",
  },
];
