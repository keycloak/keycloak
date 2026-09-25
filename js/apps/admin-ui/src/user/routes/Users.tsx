import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  type UsersParams,
  type UserTab,
  UsersRoutePath,
  UsersRouteWithTabPath,
  toUsers,
} from "./Users.routes";

export type { UserTab, UsersParams };
export { toUsers };

const UsersSection = lazy(() => import("../UsersSection"));

export const UsersRoute: AppRouteObject = {
  path: UsersRoutePath,
  element: <UsersSection />,
  handle: {
    access: "query-users",
    breadcrumb: (t) => t("titleUsers"),
  },
};

export const UsersRouteWithTab: AppRouteObject = {
  ...UsersRoute,
  path: UsersRouteWithTabPath,
};
