import { lazy } from "react";
import type { Path } from "react-router-dom";
import { generateEncodedPath } from "../utils/generateEncodedPath";
import type { AppRouteObject } from "../routes";

export type RealmParams = { realm: string };

const RealmSection = lazy(() => import("./RealmSection"));

export const RealmRoute: AppRouteObject = {
  path: "/:realm/realms",
  element: <RealmSection />,
  breadcrumb: (t) => t("realms"),
  handle: {
    access: "anyone",
  },
};

export const toRealm = (params: RealmParams): Partial<Path> => ({
  pathname: generateEncodedPath(RealmRoute.path, params),
});
