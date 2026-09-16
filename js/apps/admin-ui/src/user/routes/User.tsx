import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  type UserParams,
  type UserTab,
  UserRoutePath,
  toUser,
} from "./User.routes";

export type { UserParams, UserTab };
export { toUser };

const EditUser = lazy(() => import("../EditUser"));

export const UserRoute: AppRouteObject = {
  path: UserRoutePath,
  element: <EditUser />,
  handle: {
    access: "query-users",
    breadcrumb: (t) => t("userDetails"),
  },
};
