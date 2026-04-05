import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";

import type { AppRouteObject } from "../../routes";

export type AddUserParams = { realm: string };

const CreateUser = lazy(() => import("../CreateUser"));

export const AddUserRoute: AppRouteObject = {
  path: "/:realm/users/add-user",
  element: <CreateUser />,
  breadcrumb: (t) => t("createUser"),
  handle: {
    access: ["query-users", "query-groups"],
  },
};

export const toAddUser = (params: AddUserParams): Partial<Path> => ({
  pathname: generateEncodedPath(AddUserRoute.path, params),
});
