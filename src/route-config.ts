import type { TFunction } from "i18next";
import type { AccessType } from "keycloak-admin/lib/defs/whoAmIRepresentation";
import type { ComponentType } from "react";
import type { MatchOptions } from "use-react-router-breadcrumbs";
import authenticationRoutes from "./authentication/routes";
import clientScopesRoutes from "./client-scopes/routes";
import clientRoutes from "./clients/routes";
import { DashboardSection } from "./dashboard/Dashboard";
import eventRoutes from "./events/routes";
import { GroupsSection } from "./groups/GroupsSection";
import { SearchGroups } from "./groups/SearchGroups";
import identityProviders from "./identity-providers/routes";
import { PageNotFoundSection } from "./PageNotFoundSection";
import realmRoleRoutes from "./realm-roles/routes";
import realmSettingRoutes from "./realm-settings/routes";
import realmRoutes from "./realm/routes";
import sessionRoutes from "./sessions/routes";
import userFederationRoutes from "./user-federation/routes";
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
  ...eventRoutes,
  ...identityProviders,
  ...realmRoleRoutes,
  ...realmRoutes,
  ...realmSettingRoutes,
  ...sessionRoutes,
  ...userFederationRoutes,
  ...userRoutes,
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
