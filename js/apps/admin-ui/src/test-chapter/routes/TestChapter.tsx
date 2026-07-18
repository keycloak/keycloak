import type { Path } from "react-router-dom";
import { AppRouteObject } from "../../routes";
import { generateEncodedPath } from "../../utils/generateEncodedPath";
import { lazy } from "react";

export type TestChapterTab = "first-tab" | "second-tab";

export type TestChapterParams = {
  realm: string;
  tab?: TestChapterTab;
};

const TestChapterSection = lazy(() => import("../TestChapterSection"));

export const TestChapterRoute: AppRouteObject = {
  path: "/:realm/test-chapter",
  element: <TestChapterSection />,
  handle: {
    access: "view-users",
    breadcrumb: (t) => t("titleTestChapter"),
  },
};

export const TestChapterRouteWithTab: AppRouteObject = {
  ...TestChapterRoute,
  path: "/:realm/test-chapter/:tab",
};

export const toTestChapter = (params: TestChapterParams): Partial<Path> => {
  const path = params.tab
    ? TestChapterRouteWithTab.path
    : TestChapterRoute.path;

  return {
    pathname: generateEncodedPath(path, params),
  };
};
