import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type AesGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const AesGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/aes-generated/settings",
  component: lazy(
    () => import("../key-providers/aes-generated/AESGeneratedForm")
  ),
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toAesGeneratedSettings = (
  params: AesGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(AesGeneratedSettingsRoute.path, params),
});
