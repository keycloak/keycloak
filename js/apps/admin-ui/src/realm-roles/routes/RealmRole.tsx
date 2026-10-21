import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

import type { AppRouteObject } from "../../routes";

export type RealmRoleTab =
  | "details"
  | "associated-roles"
  | "attributes"
  | "users-in-role"
  | "permissions"
  | "events";

export type RealmRoleParams = {
  realm: string;
  id: string;
  tab: RealmRoleTab;
};

const RealmRoleTabs = lazy(() => import("../RealmRoleTabs"));

export const RealmRoleRoute: AppRouteObject = {
  path: "/:realm/roles/:id/:tab",
  element: <RealmRoleTabs />,
  handle: {
    access: ["view-realm", "view-users"],
    breadcrumb: (t) => t("roleDetails"),
  },
};

export const toRealmRole = (params: RealmRoleParams): Partial<Path> => ({
  pathname: generateEncodedPath(RealmRoleRoute.path, params),
});
