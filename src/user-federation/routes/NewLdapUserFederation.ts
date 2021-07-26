import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UserFederationLdapSettings } from "../UserFederationLdapSettings";

export type NewLdapUserFederationParams = { realm: string };

export const NewLdapUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/new",
  component: UserFederationLdapSettings,
  breadcrumb: (t) => t("user-federation:addOneLdap"),
  access: "view-realm",
};

export const toNewLdapUserFederation = (
  params: NewLdapUserFederationParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewLdapUserFederationRoute.path, params),
});
