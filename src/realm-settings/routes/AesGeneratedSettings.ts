import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { AESGeneratedSettings } from "../key-providers/aes-generated/AESGeneratedForm";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type AesGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const AesGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/aes-generated/settings",
  component: AESGeneratedSettings,
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toAesGeneratedSettings = (
  params: AesGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(AesGeneratedSettingsRoute.path, params),
});
