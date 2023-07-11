import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewLdapUserFederationParams = { realm: string };

const CreateUserFederationLdapSettings = lazy(
  () => import("../CreateUserFederationLdapSettings"),
);

export const NewLdapUserFederationRoute: AppRouteObject = {
  path: "/:realm/user-federation/ldap/new",
  element: <CreateUserFederationLdapSettings />,
  breadcrumb: (t) =>
    t("user-federation:addProvider", { provider: "LDAP", count: 1 }),
  handle: {
    access: "view-realm",
  },
};

export const toNewLdapUserFederation = (
  params: NewLdapUserFederationParams,
): Partial<Path> => ({
  pathname: generatePath(NewLdapUserFederationRoute.path, params),
});
