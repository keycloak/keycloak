import PolicyResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyResultRepresentation";
import ScopeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/scopeRepresentation";
import PolicyEvaluationResponse from "@keycloak/keycloak-admin-client/lib/defs/policyEvaluationResponse";
import { Alert, List, ListItem, Text } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

type PermissionEvaluationResultProps = {
  evaluateResult: PolicyEvaluationResponse;
  evaluatedAllowedScopes: ScopeRepresentation[];
  evaluatedDeniedScopes: ScopeRepresentation[];
  evaluatedPolicies: PolicyResultRepresentation[];
};

export const PermissionEvaluationResult = ({
  evaluateResult,
  evaluatedAllowedScopes,
  evaluatedDeniedScopes,
  evaluatedPolicies,
}: PermissionEvaluationResultProps) => {
  const { t } = useTranslation();
  const alertTitle = evaluateResult?.results?.[0]?.resource?.name!;
  const alertVariant =
    evaluateResult?.status === "PERMIT" ? "success" : "warning";
  const alertMessage =
    evaluateResult?.status === "PERMIT" ? t("grantedScope") : t("deniedScope");

  return (
    <Alert isInline variant={alertVariant} title={alertTitle} component="h6">
      <Text>{alertMessage}</Text>

      {evaluateResult?.status === "PERMIT" &&
        evaluatedAllowedScopes.length > 0 && (
          <List className="pf-v5-u-mt-sm">
            {evaluatedAllowedScopes.map((scope) => (
              <ListItem key={scope.id}>{scope.name}</ListItem>
            ))}
          </List>
        )}

      {evaluatedDeniedScopes.length > 0 && (
        <List className="pf-v5-u-mt-sm">
          {evaluatedDeniedScopes.map((scope) => (
            <ListItem key={scope.id}>{scope.name}</ListItem>
          ))}
        </List>
      )}

      {evaluatedPolicies.length > 0 && (
        <Text className="pf-v5-u-mt-sm">
          {evaluatedPolicies.map((evaluatedPolicy) => (
            <Text key={evaluatedPolicy.policy?.id}>
              {t("evaluatedPolicy", {
                name: evaluatedPolicy.policy?.name,
                status: evaluatedPolicy.status,
              })}
            </Text>
          ))}
        </Text>
      )}
    </Alert>
  );
};
