import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RealmRolesParams = { realm: string };

const RealmRolesSection = lazy(() => import("../RealmRolesSection"));

export const RealmRolesRoute: RouteDef = {
  path: "/:realm/roles",
  element: <RealmRolesSection />,
  breadcrumb: (t) => t("roles:realmRolesList"),
  access: "view-realm",
};

export const toRealmRoles = (params: RealmRolesParams): Partial<Path> => ({
  pathname: generatePath(RealmRolesRoute.path, params),
});
