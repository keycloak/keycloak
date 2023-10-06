import { Path, generatePath } from "react-router-dom";
import type { AppRouteObject } from "../routes";
import { lazy } from "react";

export type PageParams = { pageId: string };

const Page = lazy(() => import("./Page"));

const PageRoute: AppRouteObject = {
  path: "/:realm/page", //:pageId
  element: <Page />,
  breadcrumb: (t) => t("page"),
  handle: {
    access: "view-realm",
  },
};

const routes: AppRouteObject[] = [PageRoute];

export const toPage = (params: PageParams): Partial<Path> => ({
  pathname: generatePath(PageRoute.path, params),
});

export default routes;
