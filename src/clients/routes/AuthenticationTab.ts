import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AuthorizationTab =
  | "settings"
  | "resources"
  | "scopes"
  | "policies"
  | "permissions"
  | "evaluate";

export type AuthorizationParams = {
  realm: string;
  clientId: string;
  tab: AuthorizationTab;
};
export const AuthorizationRoute: RouteDef = {
  path: "/:realm/clients/:clientId/authorization/:tab",
  component: lazy(() => import("../ClientDetails")),
  breadcrumb: (t) => t("clients:clientSettings"),
  access: "view-clients",
};

export const toAuthorizationTab = (
  params: AuthorizationParams
): LocationDescriptorObject => ({
  pathname: generatePath(AuthorizationRoute.path, params),
});
