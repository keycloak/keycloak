import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AuthenticationSection } from "../AuthenticationSection";

export type AuthenticationParams = { realm: string };

export const AuthenticationRoute: RouteDef = {
  path: "/:realm/authentication",
  component: AuthenticationSection,
  breadcrumb: (t) => t("authentication"),
  access: "view-realm",
};

export const toAuthentication = (
  params: AuthenticationParams
): LocationDescriptorObject => ({
  pathname: generatePath(AuthenticationRoute.path, params),
});
