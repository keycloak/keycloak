import { lazy } from "react";
import type { AppRouteObject } from "../../routes";

const AddOAuth2 = lazy(() => import("../add/AddOAuth2"));

export const IdentityProviderOAuth2Route: AppRouteObject = {
  path: "/:realm/identity-providers/oauth2/add",
  element: <AddOAuth2 />,
  breadcrumb: (t) => t("addOAuth2Provider"),
  handle: {
    access: "manage-identity-providers",
  },
};
