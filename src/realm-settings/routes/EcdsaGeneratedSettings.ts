import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { ECDSAGeneratedSettings } from "../key-providers/ecdsa-generated/ECDSAGeneratedForm";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type EcdsaGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const EcdsaGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/ecdsa-generated/settings",
  component: ECDSAGeneratedSettings,
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toEcdsaGeneratedSettings = (
  params: EcdsaGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(EcdsaGeneratedSettingsRoute.path, params),
});
