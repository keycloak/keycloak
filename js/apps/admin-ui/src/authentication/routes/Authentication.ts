import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AuthenticationTab = "flows" | "required-actions" | "policies";

export type AuthenticationParams = { realm: string; tab?: AuthenticationTab };

export const AuthenticationRoute: RouteDef = {
  path: "/:realm/authentication",
  component: lazy(() => import("../AuthenticationSection")),
  breadcrumb: (t) => t("authentication"),
  access: ["view-realm", "view-identity-providers", "view-clients"],
};

export const AuthenticationRouteWithTab: RouteDef = {
  ...AuthenticationRoute,
  path: "/:realm/authentication/:tab",
};

export const toAuthentication = (
  params: AuthenticationParams
): Partial<Path> => {
  const path = params.tab
    ? AuthenticationRouteWithTab.path
    : AuthenticationRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
