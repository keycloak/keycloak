import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { AppRouteObject } from "../../routes";

export type NewKerberosUserFederationParams = { realm: string };

const UserFederationKerberosSettings = lazy(
  () => import("../UserFederationKerberosSettings"),
);

export const NewKerberosUserFederationRoute: AppRouteObject = {
  path: "/:realm/user-federation/kerberos/new",
  element: <UserFederationKerberosSettings />,
  breadcrumb: (t) => t("settings"),
  handle: {
    access: "view-realm",
  },
};

export const toNewKerberosUserFederation = (
  params: NewKerberosUserFederationParams,
): Partial<Path> => ({
  pathname: generatePath(NewKerberosUserFederationRoute.path, params),
});
