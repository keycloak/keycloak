import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { Path } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

type UserFederationLdapTab = "settings" | "mappers";

export type UserFederationLdapParams = {
  realm: string;
  id: string;
  tab?: UserFederationLdapTab;
};

export const UserFederationLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/:id/:tab?",
  component: lazy(() => import("../UserFederationLdapSettings")),
  breadcrumb: (t) => t("common:settings"),
  access: "view-realm",
  legacy: true,
};

export const toUserFederationLdap = (
  params: UserFederationLdapParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationLdapRoute.path, params),
});
