import type { AppRouteObject } from "../routes";
import { WorkflowsRoute } from "./routes/Workflows";
import { WorkflowDetailRoute } from "./routes/WorkflowDetail";

const routes: AppRouteObject[] = [WorkflowsRoute, WorkflowDetailRoute];

export default routes;
