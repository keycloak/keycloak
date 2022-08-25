import type { RouteDef } from "../route-config";
import {
  AuthenticationRoute,
  AuthenticationRouteWithTab,
} from "./routes/Authentication";
import { CreateFlowRoute } from "./routes/CreateFlow";
import { FlowRoute, FlowWithBuiltInRoute } from "./routes/Flow";

const routes: RouteDef[] = [
  AuthenticationRoute,
  AuthenticationRouteWithTab,
  CreateFlowRoute,
  FlowRoute,
  FlowWithBuiltInRoute,
];

export default routes;
