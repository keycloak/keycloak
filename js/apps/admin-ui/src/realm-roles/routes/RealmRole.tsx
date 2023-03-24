import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { RouteDef } from "../../route-config";

export type RealmRoleTab =
  | "details"
  | "associated-roles"
  | "attributes"
  | "users-in-role"
  | "permissions";

export type RealmRoleParams = {
  realm: string;
  id: string;
  tab: RealmRoleTab;
};

const RealmRoleTabs = lazy(() => import("../RealmRoleTabs"));

export const RealmRoleRoute: RouteDef = {
  path: "/:realm/roles/:id/:tab",
  element: <RealmRoleTabs />,
  breadcrumb: (t) => t("roles:roleDetails"),
  access: ["view-realm", "view-users"],
};

export const toRealmRole = (params: RealmRoleParams): Partial<Path> => ({
  pathname: generatePath(RealmRoleRoute.path, params),
});
