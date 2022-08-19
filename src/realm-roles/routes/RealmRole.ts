import { lazy } from "react";
import { generatePath } from "react-router";
import type { Path } from "react-router-dom-v5-compat";
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
  path: "/:realm/roles/:id/:tab?",
  component: lazy(() => import("../RealmRoleTabs")),
  breadcrumb: (t) => t("roles:roleDetails"),
  access: ["view-realm", "view-users"],
  legacy: true,
};

export const toRealmRole = (params: RealmRoleParams): Partial<Path> => ({
  pathname: generatePath(RealmRoleRoute.path, params),
});
