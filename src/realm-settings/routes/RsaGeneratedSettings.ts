import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RsaGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const RsaGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/rsa-generated/settings",
  component: lazy(
    () => import("../key-providers/rsa-generated/RSAGeneratedForm")
  ),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toRsaGeneratedSettings = (
  params: RsaGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RsaGeneratedSettingsRoute.path, params),
});
