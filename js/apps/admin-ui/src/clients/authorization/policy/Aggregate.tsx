import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import { useParams } from "../../../utils/useParams";
import type { PolicyDetailsParams } from "../../routes/PolicyDetails";
import { DecisionStrategySelect } from "../DecisionStrategySelect";
import { ResourcesPolicySelect } from "../ResourcesPolicySelect";

export const Aggregate = () => {
  const { t } = useTranslation();
  const { id } = useParams<PolicyDetailsParams>();

  return (
    <>
      <FormGroup
        label={t("applyPolicy")}
        fieldId="policies"
        labelIcon={
          <HelpItem helpText={t("applyPolicyHelp")} fieldLabelId="policies" />
        }
      >
        <ResourcesPolicySelect name="policies" clientId={id} />
      </FormGroup>
      <DecisionStrategySelect helpLabel="policyDecisionStagey" />
    </>
  );
};
