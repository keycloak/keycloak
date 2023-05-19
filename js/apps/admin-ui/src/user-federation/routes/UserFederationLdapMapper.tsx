import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type UserFederationLdapMapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

const LdapMapperDetails = lazy(
  () => import("../ldap/mappers/LdapMapperDetails")
);

export const UserFederationLdapMapperRoute: AppRouteObject = {
  path: "/:realm/user-federation/ldap/:id/mappers/:mapperId",
  element: <LdapMapperDetails />,
  breadcrumb: (t) => t("common:mappingDetails"),
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationLdapMapper = (
  params: UserFederationLdapMapperParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationLdapMapperRoute.path, params),
});
