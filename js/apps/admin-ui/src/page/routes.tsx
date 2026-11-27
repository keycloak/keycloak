import { Path, generatePath } from "react-router-dom";
import type { AppRouteObject } from "../routes";
import { lazy } from "react";
import type { ScriptPageParams } from "./ScriptPage";

export type PageListParams = { realm?: string; providerId: string };
export type PageParams = { realm: string; providerId: string; id: string };

const PageList = lazy(() => import("./PageList"));
const Page = lazy(() => import("./Page"));
const ScriptPage = lazy(() => import("./ScriptPage"));

const PageListRoute: AppRouteObject = {
  path: "/:realm?/page-section/:providerId",
  element: <PageList />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("page"),
  },
};

const PageDetailRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/:id",
  element: <Page />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("details"),
  },
};

const AddPageDetailRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/add",
  element: <Page />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("add"),
  },
};

const ScriptPageRoute: AppRouteObject = {
  path: "/:realm?/ui-script/:providerId",
  element: <ScriptPage />,
  handle: {
    access: "view-realm",
    breadcrumb: (t) => t("page"),
  },
};

const routes: AppRouteObject[] = [
  PageDetailRoute,
  AddPageDetailRoute,
  PageListRoute,
  ScriptPageRoute,
];

export const toPage = (params: PageListParams): Partial<Path> => ({
  pathname: generatePath(PageListRoute.path, params),
});

export const toDetailPage = (params: PageParams): Partial<Path> => ({
  pathname: generatePath(PageDetailRoute.path, params),
});

export const addDetailPage = (params: Partial<PageParams>): Partial<Path> => ({
  pathname: generatePath(AddPageDetailRoute.path, params),
});

export const toScriptPage = (params: ScriptPageParams): Partial<Path> => ({
  pathname: generatePath(ScriptPageRoute.path, params),
});

export default routes;
