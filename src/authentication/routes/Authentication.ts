import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type AuthenticationTab = "flows" | "required-actions" | "policies";

export type AuthenticationParams = { realm: string; tab?: AuthenticationTab };

export const AuthenticationRoute: RouteDef = {
  path: "/:realm/authentication/:tab?",
  component: lazy(() => import("../AuthenticationSection")),
  breadcrumb: (t) => t("authentication"),
  access: ["view-realm", "view-identity-providers", "view-clients"],
  legacy: true,
};

export const toAuthentication = (
  params: AuthenticationParams
): Partial<Path> => ({
  pathname: generatePath(AuthenticationRoute.path, params),
});
