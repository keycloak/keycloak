import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UsersParams = { realm: string };

export const UsersRoute: RouteDef = {
  path: "/:realm/users",
  component: lazy(() => import("../UsersSection")),
  breadcrumb: (t) => t("users:title"),
  access: "query-users",
};

export const toUsers = (params: UsersParams): LocationDescriptorObject => ({
  pathname: generatePath(UsersRoute.path, params),
});
