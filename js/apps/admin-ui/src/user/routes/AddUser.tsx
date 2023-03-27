import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";

import type { AppRouteObject } from "../../routes";

export type AddUserParams = { realm: string };

const CreateUser = lazy(() => import("../CreateUser"));

export const AddUserRoute: AppRouteObject = {
  path: "/:realm/users/add-user",
  element: <CreateUser />,
  breadcrumb: (t) => t("users:createUser"),
  handle: {
    access: ["query-users", "query-groups"],
  },
};

export const toAddUser = (params: AddUserParams): Partial<Path> => ({
  pathname: generatePath(AddUserRoute.path, params),
});
