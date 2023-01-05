import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type UserFederationLdapTab = "settings" | "mappers";

export type UserFederationLdapParams = {
  realm: string;
  id: string;
  tab?: UserFederationLdapTab;
};

export const UserFederationLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/:id",
  component: lazy(() => import("../UserFederationLdapSettings")),
  breadcrumb: (t) => t("common:settings"),
  access: "view-realm",
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
