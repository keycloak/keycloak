import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
  pathname: generateEncodedPath(NewKerberosUserFederationRoute.path, params),
});
