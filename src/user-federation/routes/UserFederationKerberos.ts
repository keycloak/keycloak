import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserFederationKerberosParams = {
  realm: string;
  id: string;
};

export const UserFederationKerberosRoute: RouteDef = {
  path: "/:realm/user-federation/kerberos/:id",
  component: lazy(() => import("../UserFederationKerberosSettings")),
  breadcrumb: (t) => t("common:settings"),
  access: "view-realm",
};

export const toUserFederationKerberos = (
  params: UserFederationKerberosParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationKerberosRoute.path, params),
});
