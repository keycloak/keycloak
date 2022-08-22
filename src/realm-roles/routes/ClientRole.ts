import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type ClientRoleTab =
  | "details"
  | "attributes"
  | "users-in-role"
  | "associated-roles";

export type ClientRoleParams = {
  realm: string;
  clientId: string;
  id: string;
  tab?: ClientRoleTab;
};

export const ClientRoleRoute: RouteDef = {
  path: "/:realm/clients/:clientId/roles/:id",
  component: lazy(() => import("../RealmRoleTabs")),
  breadcrumb: (t) => t("roles:roleDetails"),
  access: "view-realm",
};

export const ClientRoleRouteWithTab: RouteDef = {
  ...ClientRoleRoute,
  path: "/:realm/clients/:clientId/roles/:id/:tab",
};

export const toClientRole = (params: ClientRoleParams): Partial<Path> => {
  const path = params.tab ? ClientRoleRouteWithTab.path : ClientRoleRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
