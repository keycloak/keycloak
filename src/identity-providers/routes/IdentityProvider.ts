import type { LocationDescriptorObject } from "history";
import { generatePath } from "react-router-dom";
import type { RouteDef } from "../../route-config";
import { DetailSettings } from "../add/DetailSettings";

type IdentityProviderTabs = "settings" | "mappers";

export type IdentityProviderParams = {
  realm: string;
  providerId: string;
  alias: string;
  tab: IdentityProviderTabs;
};

export const IdentityProviderRoute: RouteDef = {
  path: "/:realm/identity-providers/:providerId/:alias/:tab",
  component: DetailSettings,
  breadcrumb: (t) => t("identity-providers:providerDetails"),
  access: "manage-identity-providers",
};

export const toIdentityProvider = (
  params: IdentityProviderParams
): LocationDescriptorObject => ({
  pathname: generatePath(IdentityProviderRoute.path, params),
});
