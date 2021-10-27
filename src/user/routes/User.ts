import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserTab = "settings" | "groups" | "consents" | "attributes";

export type UserParams = {
  realm: string;
  id: string;
  tab: UserTab;
};

export const UserRoute: RouteDef = {
  path: "/:realm/users/:id/:tab",
  component: lazy(() => import("../UsersTabs")),
  breadcrumb: (t) => t("users:userDetails"),
  access: "manage-users",
};

export const toUser = (params: UserParams): LocationDescriptorObject => ({
  pathname: generatePath(UserRoute.path, params),
});
