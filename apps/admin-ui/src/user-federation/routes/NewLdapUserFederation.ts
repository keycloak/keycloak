import { lazy } from "react";
import type { Path } from "react-router-dom-v5-compat";
import { generatePath } from "react-router-dom-v5-compat";
import type { RouteDef } from "../../route-config";

export type NewLdapUserFederationParams = { realm: string };

export const NewLdapUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/new",
  component: lazy(() => import("../CreateUserFederationLdapSettings")),
  breadcrumb: (t) =>
    t("user-federation:addProvider", { provider: "LDAP", count: 1 }),
  access: "view-realm",
};

export const toNewLdapUserFederation = (
  params: NewLdapUserFederationParams
): Partial<Path> => ({
  pathname: generatePath(NewLdapUserFederationRoute.path, params),
});
