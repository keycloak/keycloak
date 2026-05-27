import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";
import type PasswordPolicyValueRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyValueRepresentation";

export type SubmittedValues = {
  [index: string]: string;
};

export const serializePolicy = (
  policies: PasswordPolicyTypeRepresentation[],
  submitted: SubmittedValues,
) =>
  policies
    .map((policy) => {
      return {id: policy.id!, value: submitted[policy.id!]};
    });

type PolicyValue = PasswordPolicyTypeRepresentation & {
  value?: string;
};

export const parsePolicy = (
  value: PasswordPolicyValueRepresentation[],
  policies: PasswordPolicyTypeRepresentation[],
) =>
  value
    .reduce<PolicyValue[]>((result, { id, value }) => {
      const matchingPolicy = policies.find((policy) => policy.id === id);

      if (!matchingPolicy) {
        return result;
      }

      return result.concat({ ...matchingPolicy, value });
    }, []);

