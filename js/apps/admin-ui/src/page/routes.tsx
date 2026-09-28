import { Path, generatePath } from "react-router-dom";
import type { AppRouteObject } from "../routes";
import { lazy } from "react";

export type PageListParams = { realm?: string; providerId: string };
export type PageParams = { realm: string; providerId: string; id: string };

const PageList = lazy(() => import("./PageList"));
const Page = lazy(() => import("./Page"));

const PageListRoute: AppRouteObject = {
  path: "/:realm?/page-section/:providerId",
  element: <PageList />,
  handle: {
    access: "anyone",
    breadcrumb: (t) => t("page"),
  },
};

const PageDetailRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/:id/:tab?",
  element: <Page />,
  handle: {
    access: "anyone",
    breadcrumb: (t) => t("details"),
  },
};

const PageDetailLegacyRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/:id",
  element: <Page />,
  handle: {
    access: "anyone",
    breadcrumb: (t) => t("details"),
  },
};

const AddPageDetailRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/add",
  element: <Page />,
  handle: {
    access: "anyone",
    breadcrumb: (t) => t("add"),
  },
};

const routes: AppRouteObject[] = [
  PageDetailRoute,
  PageDetailLegacyRoute,
  AddPageDetailRoute,
  PageListRoute,
];

export const toPage = (params: PageListParams): Partial<Path> => ({
  pathname: generatePath(PageListRoute.path, params),
});

export const toDetailPage = (
  params: PageParams & { detailTabPath?: string },
): Partial<Path> => ({
  pathname: generatePath(params.detailTabPath ?? PageDetailRoute.path, params),
});

export const addDetailPage = (params: Partial<PageParams>): Partial<Path> => ({
  pathname: generatePath(AddPageDetailRoute.path, params),
});

export default routes;
