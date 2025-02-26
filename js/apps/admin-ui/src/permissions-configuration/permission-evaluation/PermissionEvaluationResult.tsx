import PolicyEvaluationResponse from "@keycloak/keycloak-admin-client/lib/defs/policyEvaluationResponse";
import { Alert, List, ListItem, Text } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useMemo } from "react";
import { sortBy } from "lodash-es";

type PermissionEvaluationResultProps = {
  evaluateResult: PolicyEvaluationResponse;
};

export const PermissionEvaluationResult = ({
  evaluateResult,
}: PermissionEvaluationResultProps) => {
  const { t } = useTranslation();
  const alertTitle = evaluateResult?.results?.[0]?.resource?.name!;
  const alertVariant =
    evaluateResult?.status === "PERMIT" ? "success" : "warning";
  const alertMessage =
    evaluateResult?.status === "PERMIT" ? t("grantedScope") : t("deniedScope");

  const evaluatedAllowedScopes = useMemo(() => {
    return sortBy(evaluateResult?.results?.[0]?.allowedScopes || [], "name");
  }, [evaluateResult?.results]);

  const evaluatedDeniedScopes = useMemo(() => {
    return sortBy(evaluateResult?.results?.[0]?.deniedScopes || [], "name");
  }, [evaluateResult?.results]);

  const evaluatedPolicies = useMemo(() => {
    return sortBy(evaluateResult?.results?.[0]?.policies || [], "name");
  }, [evaluateResult?.results]);

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
