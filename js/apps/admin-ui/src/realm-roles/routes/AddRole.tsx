import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddRoleParams = { realm: string };

const CreateRealmRole = lazy(() => import("../CreateRealmRole"));

export const AddRoleRoute: RouteDef = {
  path: "/:realm/roles/new",
  element: <CreateRealmRole />,
  breadcrumb: (t) => t("roles:createRole"),
  access: "manage-realm",
};

export const toAddRole = (params: AddRoleParams): Partial<Path> => ({
  pathname: generatePath(AddRoleRoute.path, params),
});
