import type { AppRouteObject } from "../routes";
import { AddWorkflowRoute } from "./routes/AddWorkflow";
import { WorkflowsRoute } from "./routes/Workflows";

const routes: AppRouteObject[] = [WorkflowsRoute, AddWorkflowRoute];

export default routes;
