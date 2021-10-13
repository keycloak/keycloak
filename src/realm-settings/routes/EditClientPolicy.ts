import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewClientPolicyForm } from "../NewClientPolicyForm";

export type EditClientPolicyParams = {
  realm: string;
  policyName: string;
};

export const EditClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:policyName/edit-policy",
  component: NewClientPolicyForm,
  access: "manage-realm",
  breadcrumb: (t) => t("identity-providers:editIdPMapper"),
};

export const toEditClientPolicy = (
  params: EditClientPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(EditClientPolicyRoute.path, params),
});
