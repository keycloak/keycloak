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

  const evaluatedPermission = function (title: string, status: string) {
    const permissions = evaluatedPolicies.filter((p) => p.status === status);

    if (permissions.length == 0) {
      return;
    }

    return (
      <>
        <Text className="pf-v5-u-pt-sm">
          <strong>{t(title)}</strong>:
        </Text>
        <List className="pf-v5-u-mt-sm">
          {permissions.map((p) => (
            <ListItem key={p.policy?.id}>
              {t("evaluatedPolicy", {
                name: p.policy?.name,
                status: p.status,
              })}
            </ListItem>
          ))}
        </List>
      </>
    );
  };

  return (
    <Alert isInline variant={alertVariant} title={alertTitle} component="h6">
      {evaluatedAllowedScopes.length > 0 && (
        <>
          <Text>
            <b>{t("grantedScope")}</b>
          </Text>
          <List className="pf-v5-u-mt-sm">
            {evaluatedAllowedScopes.map((scope) => (
              <ListItem key={scope.id}>{scope.name}</ListItem>
            ))}
          </List>
        </>
      )}

      {evaluatedDeniedScopes.length > 0 && (
        <>
          <Text className="pf-v5-u-pt-sm">
            <strong>{t("deniedScope")}</strong>
          </Text>

          <List className="pf-v5-u-mt-sm">
            {evaluatedDeniedScopes.map((scope) => (
              <ListItem key={scope.id}>{scope.name}</ListItem>
            ))}
          </List>
        </>
      )}

      {evaluatedPermission("grantedPermissions", "PERMIT")}
      {evaluatedPermission("deniedPermissions", "DENY")}
    </Alert>
  );
};
