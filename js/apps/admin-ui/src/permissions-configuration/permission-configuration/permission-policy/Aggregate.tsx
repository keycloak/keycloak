import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { DecisionStrategySelect } from "../../../clients/authorization/DecisionStrategySelect";
import { ResourcesPolicySelect } from "../../../clients/authorization/ResourcesPolicySelect";

type AggregateProps = {
  permissionClientId: string;
};

export const Aggregate = ({ permissionClientId }: AggregateProps) => {
  const { t } = useTranslation();

  return (
    <>
      <FormGroup
        label={t("applyPolicy")}
        fieldId="policies"
        labelIcon={
          <HelpItem helpText={t("applyPolicyHelp")} fieldLabelId="policies" />
        }
      >
        <ResourcesPolicySelect
          name="policies"
          clientId={permissionClientId}
          isPermissionClient
        />
      </FormGroup>
      <DecisionStrategySelect helpLabel="policyDecisionStagey" />
    </>
  );
};
