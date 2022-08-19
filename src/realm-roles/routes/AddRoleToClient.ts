import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type AddRoleToClientParams = {
  realm: string;
  clientId: string;
};

export const AddRoleToClientRoute: RouteDef = {
  path: "/:realm/clients/:clientId/roles/add-role",
  component: lazy(() => import("../RealmRoleTabs")),
  breadcrumb: (t) => t("roles:createRole"),
  access: "manage-realm",
};

export const toAddRoleToClient = (
  params: AddRoleToClientParams
): Partial<Path> => ({
  pathname: generatePath(AddRoleToClientRoute.path, params),
});
