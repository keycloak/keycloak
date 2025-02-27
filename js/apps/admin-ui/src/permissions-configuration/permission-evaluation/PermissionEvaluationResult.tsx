import PolicyEvaluationResponse from "@keycloak/keycloak-admin-client/lib/defs/policyEvaluationResponse";
import { useMemo } from "react";
import { Alert, List, ListItem, Text } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { sortBy } from "lodash-es";

type PermissionEvaluationResultProps = {
  evaluateResult: PolicyEvaluationResponse;
};

export const PermissionEvaluationResult = ({
  evaluateResult,
}: PermissionEvaluationResultProps) => {
  const { t } = useTranslation();
  const evaluatedResults = evaluateResult?.results || [];
  const evaluatedResult = evaluatedResults[0] || {};
  const alertTitle =
    evaluatedResult?.resource?.name ?? t("permissionEvaluationAlertTitle");
  const alertVariant =
    evaluateResult?.status === "PERMIT" ? "success" : "warning";
  const alertMessage =
    evaluateResult?.status === "PERMIT"
      ? t("grantedScope")
      : evaluateResult?.status === "DENY" && evaluatedResults.length === 0
        ? ""
        : t("deniedScope");

  const evaluatedAllowedScopes = useMemo(
    () => sortBy(evaluatedResult?.allowedScopes || [], "name"),
    [evaluatedResult],
  );
  const evaluatedDeniedScopes = useMemo(
    () => sortBy(evaluatedResult?.deniedScopes || [], "name"),
    [evaluatedResult],
  );
  const evaluatedPolicies = useMemo(
    () => sortBy(evaluatedResult?.policies || [], "name"),
    [evaluatedResult],
  );

  return (
    <Alert isInline variant={alertVariant} title={alertTitle} component="h6">
      <Text>{alertMessage}</Text>

      {evaluatedAllowedScopes.length > 0 && (
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
