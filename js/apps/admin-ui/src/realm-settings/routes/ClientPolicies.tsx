import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateUnencodedPath } from "../../util";
import type { AppRouteObject } from "../../routes";

export type ClientPoliciesTab = "profiles" | "policies";

export type ClientPoliciesParams = {
  realm: string;
  tab: ClientPoliciesTab;
};

const RealmSettingsSection = lazy(() => import("../RealmSettingsSection"));

export const ClientPoliciesRoute: AppRouteObject = {
  path: "/:realm/realm-settings/client-policies/:tab",
  element: <RealmSettingsSection />,
  breadcrumb: (t) => t("clientPolicies"),
  handle: {
    access: "view-realm",
  },
};

export const toClientPolicies = (
  params: ClientPoliciesParams,
): Partial<Path> => ({
  pathname: generateUnencodedPath(ClientPoliciesRoute.path, params),
});
