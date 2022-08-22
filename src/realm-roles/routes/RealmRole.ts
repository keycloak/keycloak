import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type RealmRoleTab =
  | "details"
  | "associated-roles"
  | "attributes"
  | "users-in-role";

export type RealmRoleParams = {
  realm: string;
  id: string;
  tab?: RealmRoleTab;
};

export const RealmRoleRoute: RouteDef = {
  path: "/:realm/roles/:id",
  component: lazy(() => import("../RealmRoleTabs")),
  breadcrumb: (t) => t("roles:roleDetails"),
  access: ["view-realm", "view-users"],
};

export const RealmRoleRouteWithTab: RouteDef = {
  ...RealmRoleRoute,
  path: "/:realm/roles/:id/:tab",
};

export const toRealmRole = (params: RealmRoleParams): Partial<Path> => {
  const path = params.tab ? RealmRoleRouteWithTab.path : RealmRoleRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
