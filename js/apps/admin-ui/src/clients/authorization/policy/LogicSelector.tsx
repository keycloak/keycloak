import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, Radio } from "@patternfly/react-core";

import { HelpItem } from "@keycloak/keycloak-ui-shared";

const LOGIC_TYPES = ["POSITIVE", "NEGATIVE"] as const;

type LogicSelectorProps = {
  isDisabled?: boolean;
};

export const LogicSelector = ({ isDisabled }: LogicSelectorProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("logic")}
      labelIcon={<HelpItem helpText={t("logicHelp")} fieldLabelId="logic" />}
      fieldId="logic"
      hasNoPaddingTop
    >
      <Controller
        name="logic"
        data-testid="logic"
        defaultValue={LOGIC_TYPES[0]}
        control={control}
        render={({ field }) => (
          <>
            {LOGIC_TYPES.map((type) => (
              <Radio
                id={type}
                key={type}
                data-testid={type}
                isChecked={field.value === type}
                name="logic"
                onChange={() => field.onChange(type)}
                label={t(`logicType.${type.toLowerCase()}`)}
                className="pf-v5-u-mb-md"
                isDisabled={isDisabled}
              />
            ))}
          </>
        )}
      />
    </FormGroup>
  );
};
