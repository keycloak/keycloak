import { lazy } from "react";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { Path } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type UserTab = "list" | "permissions";

export type UsersParams = { realm: string; tab?: UserTab };

const UsersSection = lazy(() => import("../UsersSection"));

export const UsersRoute: AppRouteObject = {
  path: "/:realm/users",
  element: <UsersSection />,
  breadcrumb: (t) => t("titleUsers"),
  handle: {
    access: "query-users",
  },
};

export const UsersRouteWithTab: AppRouteObject = {
  ...UsersRoute,
  path: "/:realm/users/:tab",
};

export const toUsers = (params: UsersParams): Partial<Path> => {
  const path = params.tab ? UsersRouteWithTab.path : UsersRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
