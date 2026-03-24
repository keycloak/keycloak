import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
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
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("clientPolicies"),
  },
};

export const toClientPolicies = (
  params: ClientPoliciesParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(ClientPoliciesRoute.path, params),
});
