import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { RSASettings } from "../key-providers/rsa/RSAForm";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type RsaSettingsParams = {
  realm: string;
  id: string;
};

export const RsaSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/rsa/settings",
  component: RSASettings,
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toRsaSettings = (
  params: RsaSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RsaSettingsRoute.path, params),
});
