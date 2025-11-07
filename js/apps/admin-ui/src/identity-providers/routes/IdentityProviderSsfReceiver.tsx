import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import type { AppRouteObject } from "../../routes";

export type IdentityProviderSsfReceiverParams = { realm: string };

const AddSsfReceiver = lazy(() => import("../add/AddSsfReceiver"));

export const IdentityProviderSsfReceiverRoute: AppRouteObject = {
  path: "/:realm/identity-providers/ssf-receiver/add",
  element: <AddSsfReceiver />,
  breadcrumb: (t) => t("addSsfReceiverProvider"),
  handle: {
    access: "manage-identity-providers",
  },
};

export const toIdentityProviderSsfReceiver = (
  params: IdentityProviderSsfReceiverParams,
): Partial<Path> => ({
  pathname: generateEncodedPath(IdentityProviderSsfReceiverRoute.path, params),
});
