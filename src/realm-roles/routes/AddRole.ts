import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RealmRoleTabs } from "../RealmRoleTabs";

export type AddRoleParams = { realm: string };

export const AddRoleRoute: RouteDef = {
  path: "/:realm/roles/add-role",
  component: RealmRoleTabs,
  breadcrumb: (t) => t("roles:createRole"),
  access: "manage-realm",
};

export const toAddRole = (params: AddRoleParams): LocationDescriptorObject => ({
  pathname: generatePath(AddRoleRoute.path, params),
});
