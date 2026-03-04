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

export interface GroupPolicyDefinitionRepresentation {
  id?: string;
  path?: string;
  extendChildren?: boolean;
}

export interface RolePolicyRepresentation
  extends Omit<PolicyRepresentation, "type" | "config" | "roles" | "users"> {
  type?: "role";
  roles?: PolicyRoleRepresentation[];
  fetchRoles?: boolean;
  config?: never;
}

export interface GroupPolicyRepresentation
  extends Omit<PolicyRepresentation, "type" | "config" | "roles" | "users"> {
  type?: "group";
  groupsClaim?: string;
  groups?: GroupPolicyDefinitionRepresentation[];
  config?: never;
}

export interface UserPolicyRepresentation
  extends Omit<PolicyRepresentation, "type" | "config" | "roles" | "users"> {
  type?: "user";
  users?: string[];
  config?: never;
}

export interface JSPolicyRepresentation
  extends Omit<PolicyRepresentation, "type" | "config" | "roles" | "users"> {
  type?: "js";
  code?: string;
  config?: never;
}

export type TypedPolicyRepresentation<TType extends string> =
  TType extends "role"
    ? RolePolicyRepresentation
    : TType extends "group"
      ? GroupPolicyRepresentation
      : TType extends "user"
        ? UserPolicyRepresentation
        : TType extends "js"
          ? JSPolicyRepresentation
          : PolicyRepresentation;
