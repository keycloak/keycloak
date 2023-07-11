import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";

export type SubmittedValues = {
  [index: string]: string;
};

const POLICY_SEPARATOR = " and ";

export const serializePolicy = (
  policies: PasswordPolicyTypeRepresentation[],
  submitted: SubmittedValues,
) =>
  policies
    .map((policy) => `${policy.id}(${submitted[policy.id!]})`)
    .join(POLICY_SEPARATOR);

type PolicyValue = PasswordPolicyTypeRepresentation & {
  value?: string;
};

export const parsePolicy = (
  value: string,
  policies: PasswordPolicyTypeRepresentation[],
) =>
  value
    .split(POLICY_SEPARATOR)
    .map(parsePolicyToken)
    .reduce<PolicyValue[]>((result, { id, value }) => {
      const matchingPolicy = policies.find((policy) => policy.id === id);

      if (!matchingPolicy) {
        return result;
      }

      return result.concat({ ...matchingPolicy, value });
    }, []);

type PolicyTokenParsed = {
  id: string;
  value?: string;
};

function parsePolicyToken(token: string): PolicyTokenParsed {
  const valueStart = token.indexOf("(");

  if (valueStart === -1) {
    return { id: token.trim() };
  }

  const id = token.substring(0, valueStart).trim();
  const valueEnd = token.lastIndexOf(")");

  if (valueEnd === -1) {
    return { id };
  }

  const value = token.substring(valueStart + 1, valueEnd).trim();

  return { id, value };
}
