import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RSAGeneratedSettings } from "../key-providers/rsa-generated/RSAGeneratedForm";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type RsaGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const RsaGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/rsa-generated/settings",
  component: RSAGeneratedSettings,
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toRsaGeneratedSettings = (
  params: RsaGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RsaGeneratedSettingsRoute.path, params),
});
