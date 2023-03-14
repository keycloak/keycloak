import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RealmRolesParams = { realm: string };

export const RealmRolesRoute: RouteDef = {
  path: "/:realm/roles",
  component: lazy(() => import("../RealmRolesSection")),
  breadcrumb: (t) => t("roles:realmRolesList"),
  access: "view-realm",
};

export const toRealmRoles = (params: RealmRolesParams): Partial<Path> => ({
  pathname: generatePath(RealmRolesRoute.path, params),
});
