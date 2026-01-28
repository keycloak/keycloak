import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type AddRoleParams = { realm: string };

const CreateRealmRole = lazy(() => import("../CreateRealmRole"));

export const AddRoleRoute: AppRouteObject = {
  path: "/:realm/roles/new",
  element: <CreateRealmRole />,
  breadcrumb: (t) => t("createRole"),
  handle: {
    access: "manage-realm",
  },
};

export const toAddRole = (params: AddRoleParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddRoleRoute.path, params),
});
