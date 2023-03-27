import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type RealmRolesParams = { realm: string };

const RealmRolesSection = lazy(() => import("../RealmRolesSection"));

export const RealmRolesRoute: AppRouteObject = {
  path: "/:realm/roles",
  element: <RealmRolesSection />,
  breadcrumb: (t) => t("roles:realmRolesList"),
  handle: {
    access: "view-realm",
  },
};

export const toRealmRoles = (params: RealmRolesParams): Partial<Path> => ({
  pathname: generatePath(RealmRolesRoute.path, params),
});
