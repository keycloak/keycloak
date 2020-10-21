import { TFunction } from "i18next";
import { AuthenticationSection } from "./authentication/AuthenticationSection";
import { ClientScopeForm } from "./client-scopes/form/ClientScopeForm";
import { ClientScopesSection } from "./client-scopes/ClientScopesSection";
import { NewClientForm } from "./clients/add/NewClientForm";
import { ClientSettings } from "./clients/ClientSettings";
import { ClientsSection } from "./clients/ClientsSection";
import { ImportForm } from "./clients/import/ImportForm";
import { EventsSection } from "./events/EventsSection";
import { GroupsSection } from "./groups/GroupsSection";
import { IdentityProvidersSection } from "./identity-providers/IdentityProvidersSection";
import { PageNotFoundSection } from "./PageNotFoundSection";
import { NewRoleForm } from "./realm-roles/add/NewRoleForm";
import { RealmRolesSection } from "./realm-roles/RealmRolesSection";
import { RealmSettingsSection } from "./realm-settings/RealmSettingsSection";
import { NewRealmForm } from "./realm/add/NewRealmForm";
import { SessionsSection } from "./sessions/SessionsSection";
import { UserFederationSection } from "./user-federation/UserFederationSection";
import { UsersSection } from "./user/UsersSection";

import { AccessType } from "./context/whoami/who-am-i-model";

export type RouteDef = {
  path: string;
  component: () => JSX.Element;
  breadcrumb: TFunction | "";
  access: AccessType;
};

type RoutesFn = (t: TFunction) => RouteDef[];

export const routes: RoutesFn = (t: TFunction) => [
  {
    path: "/add-realm",
    component: NewRealmForm,
    breadcrumb: t("realm:createRealm"),
    access: "manage-realm",
  },
  {
    path: "/clients",
    component: ClientsSection,
    breadcrumb: t("clients:clientList"),
    access: "query-clients",
  },
  {
    path: "/clients/:id",
    component: ClientSettings,
    breadcrumb: t("clients:clientSettings"),
    access: "view-clients",
  },
  {
    path: "/add-client",
    component: NewClientForm,
    breadcrumb: t("clients:createClient"),
    access: "manage-clients",
  },
  {
    path: "/import-client",
    component: ImportForm,
    breadcrumb: t("clients:importClient"),
    access: "manage-clients",
  },
  {
    path: "/client-scopes",
    component: ClientScopesSection,
    breadcrumb: t("client-scopes:clientScopeList"),
    access: "view-clients",
  },
  {
    path: "/client-scopes/add-client-scopes",
    component: ClientScopeForm,
    breadcrumb: t("client-scopes:createClientScope"),
    access: "manage-clients",
  },
  {
    path: "/client-scopes/:id",
    component: ClientScopeForm,
    breadcrumb: t("client-scopes:clientScopeDetails"),
    access: "view-clients",
  },
  {
    path: "/roles",
    component: RealmRolesSection,
    breadcrumb: t("roles:roleList"),
    access: "view-realm",
  },
  {
    path: "/add-role",
    component: NewRoleForm,
    breadcrumb: t("roles:createRole"),
    access: "manage-realm",
  },
  {
    path: "/users",
    component: UsersSection,
    breadcrumb: t("users:title"),
    access: "query-users",
  },
  {
    path: "/groups",
    component: GroupsSection,
    breadcrumb: t("groups"),
    access: "query-groups",
  },
  {
    path: "/sessions",
    component: SessionsSection,
    breadcrumb: t("sessions:title"),
    access: "view-realm",
  },
  {
    path: "/events",
    component: EventsSection,
    breadcrumb: t("events:title"),
    access: "view-events",
  },
  {
    path: "/realm-settings",
    component: RealmSettingsSection,
    breadcrumb: t("realmSettings"),
    access: "view-realm",
  },
  {
    path: "/authentication",
    component: AuthenticationSection,
    breadcrumb: t("authentication"),
    access: "view-realm",
  },
  {
    path: "/identity-providers",
    component: IdentityProvidersSection,
    breadcrumb: t("identityProviders"),
    access: "view-identity-providers",
  },
  {
    path: "/user-federation",
    component: UserFederationSection,
    breadcrumb: t("userFederation"),
    access: "view-realm",
  },
  {
    path: "/",
    component: ClientsSection,
    breadcrumb: t("common:home"),
    access: "anyone",
  },
  {
    path: "",
    component: PageNotFoundSection,
    breadcrumb: "",
    access: "anyone",
  },
];
