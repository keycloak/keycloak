import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type RsaSettingsParams = {
  realm: string;
  id: string;
};

export const RsaSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/rsa/settings",
  component: lazy(() => import("../key-providers/rsa/RSAForm")),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toRsaSettings = (
  params: RsaSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RsaSettingsRoute.path, params),
});
