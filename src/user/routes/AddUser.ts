import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AddUserParams = { realm: string };

export const AddUserRoute: RouteDef = {
  path: "/:realm/users/add-user",
  component: lazy(() => import("../UsersTabs")),
  breadcrumb: (t) => t("users:createUser"),
  access: "manage-users",
};

export const toAddUser = (params: AddUserParams): LocationDescriptorObject => ({
  pathname: generatePath(AddUserRoute.path, params),
});
