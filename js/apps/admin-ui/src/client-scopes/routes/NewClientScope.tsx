import { lazy } from "react";
import type { AppRouteObject } from "../../routes";
import {
  NewClientScopeRoutePath,
  type NewClientScopeParams,
  toNewClientScope,
} from "./NewClientScope.routes";

export type { NewClientScopeParams };
export { toNewClientScope };

const CreateClientScope = lazy(() => import("../CreateClientScope"));

export const NewClientScopeRoute: AppRouteObject = {
  path: NewClientScopeRoutePath,
  element: <CreateClientScope />,
  handle: {
    access: "manage-clients",
    breadcrumb: (t) => t("createClientScope"),
  },
};
