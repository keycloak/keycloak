import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type KeyProviderParams = {
  realm: string;
  id: string;
};

export const AesGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/aes-generated/settings",
  component: lazy(
    () => import("../key-providers/aes-generated/AESGeneratedForm")
  ),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toAesGeneratedSettings = (
  params: KeyProviderParams
): LocationDescriptorObject => ({
  pathname: generatePath(AesGeneratedSettingsRoute.path, params),
});
