import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type UserFederationsLdapParams = { realm: string };

export const UserFederationsLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap",
  component: lazy(() => import("../UserFederationSection")),
  access: "view-realm",
};

export const toUserFederationsLdap = (
  params: UserFederationsLdapParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationsLdapRoute.path, params),
});
