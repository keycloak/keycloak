import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { JavaKeystoreSettings } from "../key-providers/java-keystore/JavaKeystoreForm";
import { EditProviderCrumb } from "../RealmSettingsSection";

export type JavaKeystoreSettingsParams = {
  realm: string;
  id: string;
};

export const JavaKeystoreSettingsRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/:id/java-keystore/settings",
  component: JavaKeystoreSettings,
  breadcrumb: () => EditProviderCrumb,
  access: "view-realm",
};

export const toJavaKeystoreSettings = (
  params: JavaKeystoreSettingsParams
): LocationDescriptorObject => ({
  pathname: generatePath(JavaKeystoreSettingsRoute.path, params),
});
