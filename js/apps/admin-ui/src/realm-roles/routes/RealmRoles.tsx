import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type RealmRolesParams = { realm: string };

const RealmRolesSection = lazy(() => import("../RealmRolesSection"));

export const RealmRolesRoute: AppRouteObject = {
  path: "/:realm/roles",
  element: <RealmRolesSection />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("realmRolesList"),
  },
};

export const toRealmRoles = (params: RealmRolesParams): Partial<Path> => ({
  pathname: generateEncodedPath(RealmRolesRoute.path, params),
});
