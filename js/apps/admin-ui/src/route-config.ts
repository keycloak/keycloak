import type { AccessType } from "@keycloak/keycloak-admin-client/lib/defs/whoAmIRepresentation";
import type { TFunction } from "i18next";
import type { ComponentType, LazyExoticComponent } from "react";

import authenticationRoutes from "./authentication/routes";
import clientScopesRoutes from "./client-scopes/routes";
import clientRoutes from "./clients/routes";
import dashboardRoutes from "./dashboard/routes";
import eventRoutes from "./events/routes";
import groupsRoutes from "./groups/routes";
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
  component: ComponentType | LazyExoticComponent<() => JSX.Element>;
  breadcrumb?: (t: TFunction) => string | ComponentType<any>;
  access: AccessType | AccessType[];
};

const NotFoundRoute: RouteDef = {
  path: "*",
  component: PageNotFoundSection,
  access: "anyone",
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
  ...groupsRoutes,
  ...dashboardRoutes,
  NotFoundRoute,
];
