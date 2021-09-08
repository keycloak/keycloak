import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { DetailSettings } from "../add/DetailSettings";

export type IdentityProviderTab = "settings";

export type IdentityProviderTabParams = {
  realm: string;
  providerId?: string;
  alias: string;
  tab?: IdentityProviderTab;
};

export const IdentityProviderTabRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId?/:alias/:tab?",
  component: DetailSettings,
  access: "manage-identity-providers",
};

export const toIdentityProviderTab = (
  params: IdentityProviderTabParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderTabRoute.path, params),
});
