import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type RsaSettingsParams = {
  realm: string;
  id: string;
};

export const RsaSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/rsa/settings",
  component: lazy(() => import("../key-providers/rsa/RSAForm")),
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toRsaSettings = (
  params: RsaSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(RsaSettingsRoute.path, params),
});
