import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EditPolicyCrumb } from "../RealmSettingsSection";

export type EditClientPolicyParams = {
  realm: string;
  policyName: string;
};

export const EditClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:policyName/edit-policy",
  component: lazy(() => import("../NewClientPolicyForm")),
  access: "manage-realm",
  breadcrumb: () => EditPolicyCrumb,
};

export const toEditClientPolicy = (
  params: EditClientPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(EditClientPolicyRoute.path, params),
});
