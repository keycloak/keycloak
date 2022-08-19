import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
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

export const toUsers = (params: UsersParams): Partial<Path> => ({
  pathname: generatePath(UsersRoute.path, params),
});
