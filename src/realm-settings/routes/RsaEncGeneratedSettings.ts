import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RsaGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const RsaEncGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/rsa-enc-generated/settings",
  component: lazy(
    () => import("../key-providers/rsa-generated/RSAGeneratedForm")
  ),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toRsaEncGeneratedSettings = (
  params: RsaGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RsaEncGeneratedSettingsRoute.path, params),
});
