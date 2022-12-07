import { FormGroup } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

import { HelpItem } from "../../../components/help-enabler/HelpItem";
import { useParams } from "../../../utils/useParams";
import type { PolicyDetailsParams } from "../../routes/PolicyDetails";
import { DecisionStrategySelect } from "../DecisionStragegySelect";
import { ResourcesPolicySelect } from "../ResourcesPolicySelect";

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
