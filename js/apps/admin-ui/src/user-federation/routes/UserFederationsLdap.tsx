import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
  pathname: generateEncodedPath(UserFederationsLdapRoute.path, params),
});
