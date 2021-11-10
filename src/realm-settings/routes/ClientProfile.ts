import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EditProfileCrumb } from "../RealmSettingsSection";

export type ClientProfileParams = {
  realm: string;
  profileName: string;
};

export const ClientProfileRoute: RouteDef = {
  path: "/:realm/realm-settings/clientPolicies/:profileName",
  component: lazy(() => import("../ClientProfileForm")),
  breadcrumb: () => EditProfileCrumb,
  access: ["view-realm", "view-users"],
};

export const toClientProfile = (
  params: ClientProfileParams
): LocationDescriptorObject => ({
  pathname: generatePath(ClientProfileRoute.path, params),
});
