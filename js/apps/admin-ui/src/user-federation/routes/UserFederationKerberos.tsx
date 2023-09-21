import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type UserFederationKerberosParams = {
  realm: string;
  id: string;
};

const UserFederationKerberosSettings = lazy(
  () => import("../UserFederationKerberosSettings"),
);

export const UserFederationKerberosRoute: AppRouteObject = {
  path: "/:realm/user-federation/kerberos/:id",
  element: <UserFederationKerberosSettings />,
  breadcrumb: (t) => t("settings"),
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationKerberos = (
  params: UserFederationKerberosParams,
): Partial<Path> => ({
  pathname: generatePath(UserFederationKerberosRoute.path, params),
});
