import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationParams = { realm: string };

export const UserFederationRoute: RouteDef = {
  path: "/:realm/user-federation",
  component: lazy(() => import("../UserFederationSection")),
  breadcrumb: (t) => t("userFederation"),
  access: "view-realm",
};

export const toUserFederation = (
  params: UserFederationParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationRoute.path, params),
});
