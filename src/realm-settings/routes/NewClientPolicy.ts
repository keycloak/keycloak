import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewClientPolicyForm } from "../NewClientPolicyForm";

export type NewClientPolicyParams = { realm: string };

export const NewClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/new-client-policy",
  component: NewClientPolicyForm,
  breadcrumb: (t) => t("realm-settings:createPolicy"),
  access: "manage-clients",
};

export const toNewClientPolicy = (
  params: NewClientPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewClientPolicyRoute.path, params),
});
