import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewClientPolicyForm } from "../NewClientPolicyForm";
import { NewPolicyCrumb } from "../RealmSettingsSection";

export type AddClientPolicyParams = { realm: string };

export const AddClientPolicyRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/add-client-policy",
  component: NewClientPolicyForm,
  breadcrumb: () => NewPolicyCrumb,
  access: "manage-clients",
};

export const toAddClientPolicy = (
  params: AddClientPolicyParams
): LocationDescriptorObject => ({
  pathname: generatePath(AddClientPolicyRoute.path, params),
});
