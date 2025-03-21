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
  breadcrumb: (t) => t("page"),
  handle: {
    access: "view-realm",
  },
};

const PageDetailRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/:id",
  element: <Page />,
  breadcrumb: (t) => t("details"),
  handle: {
    access: "view-realm",
  },
};

const AddPageDetailRoute: AppRouteObject = {
  path: "/:realm/page-section/:providerId/add",
  element: <Page />,
  breadcrumb: (t) => t("add"),
  handle: {
    access: "view-realm",
  },
};

const routes: AppRouteObject[] = [
  PageDetailRoute,
  AddPageDetailRoute,
  PageListRoute,
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

export default routes;
