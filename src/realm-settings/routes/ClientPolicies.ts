import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ProfilesTab } from "../ProfilesTab";

export type ClientPoliciesParams = {
  realm: string;
};

export const ClientPoliciesRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies",
  component: ProfilesTab,
  breadcrumb: (t) => t("realm-settings:allClientPolicies"),
  access: "view-realm",
};

export const toClientPolicies = (
  params: ClientPoliciesParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientPoliciesRoute.path, params),
});
