import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type AddRoleParams = { realm: string };

export const AddRoleRoute: RouteDef = {
  path: "/:realm/roles/add-role",
  component: lazy(() => import("../RealmRoleTabs")),
  breadcrumb: (t) => t("roles:createRole"),
  access: "manage-realm",
};

export const toAddRole = (params: AddRoleParams): Partial<Path> => ({
  pathname: generatePath(AddRoleRoute.path, params),
});
