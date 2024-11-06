import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

import type { AppRouteObject } from "../../routes";

export type AddUserParams = { realm: string };

const CreateUser = lazy(() => import("../CreateUser"));
const CreateAdminUser = lazy(() => import("../CreateAdminUser"));

export const AddUserRoute: AppRouteObject = {
  path: "/:realm/users/add-user",
  element: <CreateUser />,
  breadcrumb: (t) => t("createUser"),
  handle: {
    access: ["query-users", "query-groups"],
  },
};

export const AddAdminUserRoute: AppRouteObject = {
  path: "/:realm/users/add-admin-user",
  element: <CreateAdminUser />,
  breadcrumb: (t) => t("createUser"),
  handle: {
    access: ["query-users"],
  },
};

export const toAddAdminUser = (params: AddUserParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddAdminUserRoute.path, params),
});

export const toAddUser = (params: AddUserParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddUserRoute.path, params),
});
