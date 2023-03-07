import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationsKerberosParams = { realm: string };

export const UserFederationsKerberosRoute: RouteDef = {
  path: "/:realm/user-federation/kerberos",
  component: lazy(() => import("../UserFederationSection")),
  access: "view-realm",
};

export const toUserFederationsKerberos = (
  params: UserFederationsKerberosParams
): Partial<Path> => ({
  pathname: generatePath(UserFederationsKerberosRoute.path, params),
});
