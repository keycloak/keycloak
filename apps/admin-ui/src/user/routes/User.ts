import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type UserTab =
  | "settings"
  | "groups"
  | "consents"
  | "attributes"
  | "sessions"
  | "credentials"
  | "role-mapping"
  | "identity-provider-links";

export type UserParams = {
  realm: string;
  id: string;
  tab: UserTab;
};

export const UserRoute: RouteDef = {
  path: "/:realm/users/:id/:tab",
  component: lazy(() => import("../UsersTabs")),
  breadcrumb: (t) => t("users:userDetails"),
  access: "query-users",
};

export const toUser = (params: UserParams): Partial<Path> => ({
  pathname: generatePath(UserRoute.path, params),
});
