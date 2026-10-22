import { Path, generatePath } from "react-router-dom";
import type { AppRouteObject } from "../routes";
import { lazy } from "react";

export type UiScriptParams = { realm: string; providerId: string };

const ScriptPage = lazy(() => import("./ScriptPage"));

const UiScriptRoute: AppRouteObject = {
  path: "/:realm?/ui-script/:providerId",
  element: <ScriptPage />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("page"),
  },
};

const routes: AppRouteObject[] = [UiScriptRoute];

export const toUiScript = (params: {
  realm?: string;
  providerId: string;
}): Partial<Path> => ({
  pathname: generatePath(UiScriptRoute.path, params),
});

export default routes;
