import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationLdapTab = "settings" | "mappers";

export type UserFederationLdapParams = {
  realm: string;
  id: string;
  tab?: UserFederationLdapTab;
};

const UserFederationLdapSettings = lazy(
  () => import("../UserFederationLdapSettings")
);

export const UserFederationLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/:id",
  element: <UserFederationLdapSettings />,
  breadcrumb: (t) => t("common:settings"),
  handle: {
    access: "view-realm",
  },
};

export const UserFederationLdapWithTabRoute: RouteDef = {
  ...UserFederationLdapRoute,
  path: "/:realm/user-federation/ldap/:id/:tab",
};

export const toUserFederationLdap = (
  params: UserFederationLdapParams
): Partial<Path> => {
  const path = params.tab
    ? UserFederationLdapWithTabRoute.path
    : UserFederationLdapRoute.path;

  return {
    pathname: generatePath(path, params),
  };
};
