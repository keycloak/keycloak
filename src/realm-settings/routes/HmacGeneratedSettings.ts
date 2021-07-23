import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { HMACGeneratedSettings } from "../key-providers/hmac-generated/HMACGeneratedForm";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type HmacGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const HmacGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/hmac-generated/settings",
  component: HMACGeneratedSettings,
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toHmacGeneratedSettings = (
  params: HmacGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(HmacGeneratedSettingsRoute.path, params),
});
