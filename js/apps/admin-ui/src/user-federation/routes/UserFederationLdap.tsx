import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type UserFederationLdapTab = "settings" | "mappers";

export type UserFederationLdapParams = {
  realm: string;
  id: string;
  tab?: UserFederationLdapTab;
};

const UserFederationLdapSettings = lazy(
  () => import("../UserFederationLdapSettings"),
);

export const UserFederationLdapRoute: AppRouteObject = {
  path: "/:realm/user-federation/ldap/:id",
  element: <UserFederationLdapSettings />,
  breadcrumb: (t) => t("settings"),
  handle: {
    access: "view-realm",
  },
};

export const UserFederationLdapWithTabRoute: AppRouteObject = {
  ...UserFederationLdapRoute,
  path: "/:realm/user-federation/ldap/:id/:tab",
};

export const toUserFederationLdap = (
  params: UserFederationLdapParams,
): Partial<Path> => {
  const path = params.tab
    ? UserFederationLdapWithTabRoute.path
    : UserFederationLdapRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
