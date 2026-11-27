import type PolicyRepresentation from "./policyRepresentation.js";
import type { DecisionEffect } from "./policyRepresentation.js";

export default interface PolicyResultRepresentation {
  policy?: PolicyRepresentation;
  status?: DecisionEffect;
  memberScopeDecision?:
    | "GRANT"
    | "NO_GRANT"
    | "EXPLICIT_DENY"
    | "INDETERMINATE";
  associatedPolicies?: PolicyResultRepresentation[];
  scopes?: string[];
}
