import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type HmacGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const HmacGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/hmac-generated/settings",
  component: lazy(
    () => import("../key-providers/hmac-generated/HMACGeneratedForm")
  ),
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toHmacGeneratedSettings = (
  params: HmacGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(HmacGeneratedSettingsRoute.path, params),
});
