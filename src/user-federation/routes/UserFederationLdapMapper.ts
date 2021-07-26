import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { LdapMapperDetails } from "../ldap/mappers/LdapMapperDetails";

export type UserFederationLdapMapperParams = {
  realm: string;
  id: string;
  tab: string;
  mapperId: string;
};

export const UserFederationLdapMapperRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/:id/:tab/:mapperId",
  component: LdapMapperDetails,
  breadcrumb: (t) => t("common:mappingDetails"),
  access: "view-realm",
};

export const toUserFederationLdapMapper = (
  params: UserFederationLdapMapperParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationLdapMapperRoute.path, params),
});
