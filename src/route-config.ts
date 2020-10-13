import { TFunction } from "i18next";
import { AuthenticationSection } from "./authentication/AuthenticationSection";
import { NewClientScopeForm } from "./client-scopes/add/NewClientScopeForm";
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

export const routes = (t: TFunction) => [
  {
    path: "/add-realm",
    component: NewRealmForm,
    breadcrumb: t("realm:createRealm"),
  },
  {
    path: "/clients",
    component: ClientsSection,
    breadcrumb: t("clients:clientList"),
  },
  {
    path: "/clients/:id",
    component: ClientSettings,
    breadcrumb: t("clients:clientSettings"),
  },
  {
    path: "/add-client",
    component: NewClientForm,
    breadcrumb: t("clients:createClient"),
  },
  {
    path: "/import-client",
    component: ImportForm,
    breadcrumb: t("clients:importClient"),
  },
  {
    path: "/client-scopes",
    component: ClientScopesSection,
    breadcrumb: t("client-scopes:clientScopeList"),
  },
  {
    path: "/client-scopes/add-client-scopes",
    component: NewClientScopeForm,
    breadcrumb: t("client-scopes:createClientScope"),
  },
  {
    path: "/realm-roles",
    component: RealmRolesSection,
    breadcrumb: t("roles:roleList"),
  },
  {
    path: "/add-role",
    component: NewRoleForm,
    breadcrumb: t("roles:createRole"),
  },
  {
    path: "/users",
    component: UsersSection,
    breadcrumb: t("users:title"),
  },
  {
    path: "/groups",
    component: GroupsSection,
    breadcrumb: t("groups"),
  },
  {
    path: "/sessions",
    component: SessionsSection,
    breadcrumb: t("sessions:title"),
  },
  {
    path: "/events",
    component: EventsSection,
    breadcrumb: t("events:title"),
  },
  {
    path: "/realm-settings",
    component: RealmSettingsSection,
    breadcrumb: t("realmSettings"),
  },
  {
    path: "/authentication",
    component: AuthenticationSection,
    breadcrumb: t("authentication"),
  },
  {
    path: "/identity-providers",
    component: IdentityProvidersSection,
    breadcrumb: t("identityProviders"),
  },
  {
    path: "/user-federation",
    component: UserFederationSection,
    breadcrumb: t("userFederation"),
  },
  {
    path: "/",
    component: ClientsSection,
    breadcrumb: t("common:home"),
  },
  {
    path: "",
    component: PageNotFoundSection,
    breadcrumb: "",
  },
];
