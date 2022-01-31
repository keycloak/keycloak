import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type JavaKeystoreSettingsParams = {
  realm: string;
  id: string;
};

export const JavaKeystoreSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/java-keystore/settings",
  component: lazy(
    () => import("../key-providers/java-keystore/JavaKeystoreForm")
  ),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toJavaKeystoreSettings = (
  params: JavaKeystoreSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(JavaKeystoreSettingsRoute.path, params),
});
