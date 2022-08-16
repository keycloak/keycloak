import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
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
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationLdapRoute.path, params),
});
