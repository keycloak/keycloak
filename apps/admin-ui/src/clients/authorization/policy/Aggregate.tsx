import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { FormGroup } from "@patternfly/react-core";

import type { PolicyDetailsParams } from "../../routes/PolicyDetails";
import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { ResourcesPolicySelect } from "../ResourcesPolicySelect";
import { DecisionStrategySelect } from "../DecisionStragegySelect";

export const Aggregate = () => {
  const { t } = useTranslation("clients");
  const { id } = useParams<PolicyDetailsParams>();

  return (
    <>
      <FormGroup
        label={t("applyPolicy")}
        fieldId="policies"
        labelIcon={
          <HelpItem
            helpText="clients-help:applyPolicy"
            fieldLabelId="clients:policies"
          />
        }
      >
        <ResourcesPolicySelect name="policies" clientId={id} />
      </FormGroup>
      <DecisionStrategySelect helpLabel="policyDecisionStagey" />
    </>
  );
};
