import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
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
): LocationDescriptorObject => ({
  pathname: generatePath(AuthenticationRoute.path, params),
});
