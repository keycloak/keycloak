import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UserFederationLdapSettings } from "../UserFederationLdapSettings";

export type UserFederationLdapParams = {
  realm: string;
  id: string;
  tab?: string;
};

export const UserFederationLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/:id/:tab?",
  component: UserFederationLdapSettings,
  breadcrumb: (t) => t("common:settings"),
  access: "view-realm",
};

export const toUserFederationLdap = (
  params: UserFederationLdapParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationLdapRoute.path, params),
});
