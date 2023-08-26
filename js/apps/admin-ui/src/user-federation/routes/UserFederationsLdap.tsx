import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type UserFederationsLdapParams = { realm: string };

const UserFederationSection = lazy(() => import("../UserFederationSection"));

export const UserFederationsLdapRoute: AppRouteObject = {
  path: "/:realm/user-federation/ldap",
  element: <UserFederationSection />,
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationsLdap = (
  params: UserFederationsLdapParams,
): Partial<Path> => ({
  pathname: generatePath(UserFederationsLdapRoute.path, params),
});
