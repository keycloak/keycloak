import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type UserFederationsKerberosParams = { realm: string };

const UserFederationSection = lazy(() => import("../UserFederationSection"));

export const UserFederationsKerberosRoute: AppRouteObject = {
  path: "/:realm/user-federation/kerberos",
  element: <UserFederationSection />,
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationsKerberos = (
  params: UserFederationsKerberosParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(UserFederationsKerberosRoute.path, params),
});
