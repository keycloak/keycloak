import { lazy } from "react";
import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
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
): LocationDescriptorObject => ({
  pathname: generatePath(AddRoleToClientRoute.path, params),
});
