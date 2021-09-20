import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { NewClientProfileForm } from "../NewClientProfileForm";

export type NewClientProfileParams = {
  realm: string;
};

export const NewClientProfileRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/new-client-profile",
  component: NewClientProfileForm,
  breadcrumb: (t) => t("realm-settings:newClientProfile"),
  access: "view-realm",
};

export const toNewClientProfile = (
  params: NewClientProfileParams
): LocationDescriptorObject => ({
  pathname: generatePath(NewClientProfileRoute.path, params),
});
