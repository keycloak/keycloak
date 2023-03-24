import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AuthorizationTab =
  | "settings"
  | "resources"
  | "scopes"
  | "policies"
  | "permissions"
  | "evaluate"
  | "export";

export type AuthorizationParams = {
  realm: string;
  clientId: string;
  tab: AuthorizationTab;
};

const ClientDetails = lazy(() => import("../ClientDetails"));

export const AuthorizationRoute: RouteDef = {
  path: "/:realm/clients/:clientId/authorization/:tab",
  element: <ClientDetails />,
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "view-clients",
};

export const toAuthorizationTab = (
  params: AuthorizationParams
): Partial<Path> => ({
  pathname: generatePath(AuthorizationRoute.path, params),
});
