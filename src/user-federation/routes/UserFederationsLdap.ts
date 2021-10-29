import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationsLdapParams = { realm: string };

export const UserFederationsLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap",
  component: lazy(() => import("../UserFederationSection")),
  access: "view-realm",
};

export const toUserFederationsLdap = (
  params: UserFederationsLdapParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationsLdapRoute.path, params),
});
