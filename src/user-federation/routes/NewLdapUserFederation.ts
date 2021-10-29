import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type NewLdapUserFederationParams = { realm: string };

export const NewLdapUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/new",
  component: lazy(() => import("../UserFederationLdapSettings")),
  breadcrumb: (t) => t("user-federation:addOneLdap"),
  access: "view-realm",
};

export const toNewLdapUserFederation = (
  params: NewLdapUserFederationParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewLdapUserFederationRoute.path, params),
});
