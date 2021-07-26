import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UserFederationKerberosSettings } from "../UserFederationKerberosSettings";

export type NewKerberosUserFederationParams = { realm: string };

export const NewKerberosUserFederationRoute: RouteDef = {
  path: "/:realm/user-federation/kerberos/new",
  component: UserFederationKerberosSettings,
  breadcrumb: (t) => t("common:settings"),
  access: "view-realm",
};

export const toNewKerberosUserFederation = (
  params: NewKerberosUserFederationParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewKerberosUserFederationRoute.path, params),
});
