/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_policyrepresentation
 */

export enum DecisionStrategy {
  AFFIRMATIVE = "AFFIRMATIVE",
  UNANIMOUS = "UNANIMOUS",
  CONSENSUS = "CONSENSUS",
}

export enum DecisionEffect {
  Permit = "PERMIT",
  Deny = "DENY",
}

export enum Logic {
  POSITIVE = "POSITIVE",
  NEGATIVE = "NEGATIVE",
}

export interface PolicyRoleRepresentation {
  id: string;
  required?: boolean;
}

export default interface PolicyRepresentation {
  config?: Record<string, any>;
  decisionStrategy?: DecisionStrategy;
  description?: string;
  id?: string;
  logic?: Logic;
  name?: string;
  owner?: string;
  policies?: string[];
  resources?: string[];
  scopes?: string[];
  type?: string;
  users?: string[];
  roles?: PolicyRoleRepresentation[];
  resourceType?: string;
}
