import { lazy } from "react";
import type { Path } from "react-router-dom";
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
): Partial<Path> => ({
  pathname: generatePath(UserFederationsLdapRoute.path, params),
});
