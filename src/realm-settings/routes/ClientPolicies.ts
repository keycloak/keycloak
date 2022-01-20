import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type UserProfileTab = "profiles" | "policies";

export type ClientPoliciesParams = {
  realm: string;
  tab: string;
};

export const ClientPoliciesRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:tab",
  component: lazy(() => import("../ProfilesTab")),
  breadcrumb: (t) => t("realm-settings:allClientPolicies"),
  access: "view-realm",
};

export const toClientPolicies = (
  params: ClientPoliciesParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientPoliciesRoute.path, params),
});
