import type { DecisionEffect } from "./policyRepresentation.js";
import type PolicyResultRepresentation from "./policyResultRepresentation.js";
import type ResourceRepresentation from "./resourceRepresentation.js";
import type ScopeRepresentation from "./scopeRepresentation.js";

export default interface EvaluationResultRepresentation {
  resource?: ResourceRepresentation;
  scopes?: ScopeRepresentation[];
  policies?: PolicyResultRepresentation[];
  status?: DecisionEffect;
  allowedScopes?: ScopeRepresentation[];
  deniedScopes?: ScopeRepresentation[];
}
