import { FormGroup, Radio } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";

const DECISION_STRATEGY = ["UNANIMOUS", "AFFIRMATIVE", "CONSENSUS"] as const;

type DecisionStrategySelectProps = {
  helpLabel?: string;
  isDisabled?: boolean;
  isLimited?: boolean;
};

export const DecisionStrategySelect = ({
  helpLabel,
  isDisabled = false,
  isLimited = false,
}: DecisionStrategySelectProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("decisionStrategy")}
      labelIcon={
        <HelpItem
          helpText={t(helpLabel || "decisionStrategyHelp")}
          fieldLabelId="decisionStrategy"
        />
      }
      fieldId="decisionStrategy"
      hasNoPaddingTop
    >
      <Controller
        name="decisionStrategy"
        data-testid="decisionStrategy"
        defaultValue={DECISION_STRATEGY[0]}
        control={control}
        render={({ field }) => (
          <>
            {(isLimited
              ? DECISION_STRATEGY.slice(0, 2)
              : DECISION_STRATEGY
            ).map((strategy) => (
              <Radio
                id={strategy}
                key={strategy}
                data-testid={strategy}
                isChecked={field.value === strategy}
                isDisabled={isDisabled}
                name="decisionStrategy"
                onChange={() => field.onChange(strategy)}
                label={t(`decisionStrategies.${strategy}`)}
                className="pf-v5-u-mb-md"
              />
            ))}
          </>
        )}
      />
    </FormGroup>
  );
};
