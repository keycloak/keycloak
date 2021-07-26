import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UserFederationSection } from "../UserFederationSection";

export type UserFederationParams = { realm: string };

export const UserFederationRoute: RouteDef = {
  path: "/:realm/user-federation",
  component: UserFederationSection,
  breadcrumb: (t) => t("userFederation"),
  access: "view-realm",
};

export const toUserFederation = (
  params: UserFederationParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationRoute.path, params),
});
