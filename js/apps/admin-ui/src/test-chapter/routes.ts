import type { AppRouteObject } from "../routes";
import {
  TestChapterRoute,
  TestChapterRouteWithTab,
} from "./routes/TestChapter";

const routes: AppRouteObject[] = [TestChapterRoute, TestChapterRouteWithTab];

export default routes;
