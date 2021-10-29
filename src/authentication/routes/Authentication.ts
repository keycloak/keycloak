import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type AuthenticationParams = { realm: string; tab?: string };

export const AuthenticationRoute: RouteDef = {
  path: "/:realm/authentication/:tab?",
  component: lazy(() => import("../AuthenticationSection")),
  breadcrumb: (t) => t("authentication"),
  access: "view-realm",
};

export const toAuthentication = (
  params: AuthenticationParams
): LocationDescriptorObject => ({
  pathname: generatePath(AuthenticationRoute.path, params),
});
