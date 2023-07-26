import type PolicyRepresentation from "./policyRepresentation.js";
import type { DecisionEffect } from "./policyRepresentation.js";

export default interface PolicyResultRepresentation {
  policy?: PolicyRepresentation;
  status?: DecisionEffect;
  associatedPolicies?: PolicyResultRepresentation[];
  scopes?: string[];
}
