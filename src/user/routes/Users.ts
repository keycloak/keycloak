import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserTab = "list" | "permissions";

export type UsersParams = { realm: string; tab?: UserTab };

export const UsersRoute: RouteDef = {
  path: "/:realm/users/:tab?",
  component: lazy(() => import("../UsersSection")),
  breadcrumb: (t) => t("users:title"),
  access: "query-users",
  legacy: true,
};

export const toUsers = (params: UsersParams): LocationDescriptorObject => ({
  pathname: generatePath(UsersRoute.path, params),
});
