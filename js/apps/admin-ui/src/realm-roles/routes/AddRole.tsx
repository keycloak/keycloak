import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type AddRoleParams = { realm: string };

const CreateRealmRole = lazy(() => import("../CreateRealmRole"));

export const AddRoleRoute: AppRouteObject = {
  path: "/:realm/roles/new",
  element: <CreateRealmRole />,
  breadcrumb: (t) => t("roles:createRole"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddRole = (params: AddRoleParams): Partial<Path> => ({
  pathname: generatePath(AddRoleRoute.path, params),
});
