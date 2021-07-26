import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UserFederationSection } from "../UserFederationSection";

export type UserFederationsLdapParams = { realm: string };

export const UserFederationsLdapRoute: RouteDef = {
  path: "/:realm/user-federation/ldap",
  component: UserFederationSection,
  access: "view-realm",
};

export const toUserFederationsLdap = (
  params: UserFederationsLdapParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationsLdapRoute.path, params),
});
