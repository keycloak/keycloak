import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationsKerberosParams = { realm: string };

const UserFederationSection = lazy(() => import("../UserFederationSection"));

export const UserFederationsKerberosRoute: RouteDef = {
  path: "/:realm/user-federation/kerberos",
  element: <UserFederationSection />,
  handle: {
    access: "view-realm",
  },
};

export const toUserFederationsKerberos = (
  params: UserFederationsKerberosParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationsKerberosRoute.path, params),
});
