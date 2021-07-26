import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { UserFederationSection } from "../UserFederationSection";

export type UserFederationsKerberosParams = { realm: string };

export const UserFederationsKerberosRoute: RouteDef = {
  path: "/:realm/user-federation/kerberos",
  component: UserFederationSection,
  access: "view-realm",
};

export const toUserFederationsKerberos = (
  params: UserFederationsKerberosParams
): LocationDescriptorObject => ({
  pathname: generatePath(UserFederationsKerberosRoute.path, params),
});
