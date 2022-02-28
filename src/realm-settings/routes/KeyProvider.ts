import type { LocationDescriptorObject } from "history";
import { lazy } from "react";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";

export type ProviderType =
  | "aes-generated"
  | "ecdsa-generated"
  | "hmac-generated"
  | "java-keystore"
  | "rsa"
  | "rsa-enc-generated"
  | "rsa-generated";

export type KeyProviderParams = {
  id: string;
  providerType: ProviderType;
  realm: string;
};

export const KeyProviderFormRoute: RouteDef = {
  path: "/:realm/realm-settings/keys/providers/:id/:providerType/settings",
  component: lazy(() => import("../keys/key-providers/KeyProviderForm")),
  breadcrumb: (t) => t("realm-settings:editProvider"),
  access: "view-realm",
};

export const toKeyProvider = (
  params: KeyProviderParams
): LocationDescriptorObject => ({
  pathname: generatePath(KeyProviderFormRoute.path, params),
});
