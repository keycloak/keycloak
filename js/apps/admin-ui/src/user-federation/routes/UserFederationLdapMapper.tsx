import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type UserFederationLdapMapperParams = {
  realm: string;
  id: string;
  mapperId: string;
};

const LdapMapperDetails = lazy(
  () => import("../ldap/mappers/LdapMapperDetails"),
);

export const UserFederationLdapMapperRoute: AppRouteObject = {
  path: "/:realm/user-federation/ldap/:id/mappers/:mapperId",
  element: <LdapMapperDetails />,
  breadcrumb: (t) => t("mappingDetails"),
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationLdapMapper = (
  params: UserFederationLdapMapperParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(UserFederationLdapMapperRoute.path, params),
});
