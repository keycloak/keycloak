import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type ProviderType =
  | "aes-generated"
  | "ecdsa-generated"
  | "hmac-generated"
  | "java-keystore"
  | "rsa"
  | "rsa-enc"
  | "rsa-enc-generated"
  | "rsa-generated";

export type KeyProviderParams = {
  id: string;
  providerType: ProviderType;
  realm: string;
};

const KeyProviderForm = lazy(
  () => import("../keys/key-providers/KeyProviderForm"),
);

export const KeyProviderFormRoute: AppRouteObject = {
  path: "/:realm/realm-settings/keys/providers/:id/:providerType/settings",
  element: <KeyProviderForm />,
  breadcrumb: (t) => t("editProvider"),
  handle: {
    access: "view-realm",
  },
};

export const toKeyProvider = (params: KeyProviderParams): Partial<Path> => ({
  pathname: generateEncodedPath(KeyProviderFormRoute.path, params),
});
