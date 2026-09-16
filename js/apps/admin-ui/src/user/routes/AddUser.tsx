import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  AddUserRoutePath,
  type AddUserParams,
  toAddUser,
} from "./AddUser.routes";

export type { AddUserParams };
export { toAddUser };

const CreateUser = lazy(() => import("../CreateUser"));

export const AddUserRoute: AppRouteObject = {
  path: AddUserRoutePath,
  element: <CreateUser />,
  handle: {
    access: ["query-users", "query-groups"],
    breadcrumb: (t) => t("createUser"),
  },
};
