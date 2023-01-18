import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationLdapMapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

export const UserFederationLdapMapperRoute: RouteDef = {
  path: "/:realm/user-federation/ldap/:id/mappers/:mapperId",
  component: lazy(() => import("../ldap/mappers/LdapMapperDetails")),
  breadcrumb: (t) => t("common:mappingDetails"),
  access: "view-realm",
};

export const toUserFederationLdapMapper = (
  params: UserFederationLdapMapperParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationLdapMapperRoute.path, params),
});
