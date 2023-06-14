import type AccessTokenRepresentation from "./accessTokenRepresentation.js";
import type EvaluationResultRepresentation from "./evaluationResultRepresentation.js";
import type { DecisionEffect } from "./policyRepresentation.js";

export default interface PolicyEvaluationResponse {
  results?: EvaluationResultRepresentation[];
  entitlements?: boolean;
  status?: DecisionEffect;
  rpt?: AccessTokenRepresentation;
}
