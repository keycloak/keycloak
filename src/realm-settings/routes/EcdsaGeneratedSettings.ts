import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type EcdsaGeneratedSettingsParams = {
  realm: string;
  id: string;
};

export const EcdsaGeneratedSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/ecdsa-generated/settings",
  component: lazy(
    () => import("../key-providers/ecdsa-generated/ECDSAGeneratedForm")
  ),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toEcdsaGeneratedSettings = (
  params: EcdsaGeneratedSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(EcdsaGeneratedSettingsRoute.path, params),
});
