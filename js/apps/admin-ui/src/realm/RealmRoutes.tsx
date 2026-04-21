import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../utils/generateEncodedPath";
import type { AppRouteObject } from "../routes";

export type RealmParams = { realm: string };

const RealmSection = lazy(() => import("./RealmSection"));

export const RealmRoute: AppRouteObject = {
  path: "/:realm/realms",
  element: <RealmSection />,
  handle: {
    access: "anyone",
    breadcrumb: (t) => t("realms"),
  },
};

export const toRealm = (params: RealmParams): Partial<Path> => ({
  pathname: generateEncodedPath(RealmRoute.path, params),
});
