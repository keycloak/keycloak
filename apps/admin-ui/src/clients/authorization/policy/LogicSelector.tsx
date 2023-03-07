import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, Radio } from "@patternfly/react-core";

import { HelpItem } from "ui-shared";

const LOGIC_TYPES = ["POSITIVE", "NEGATIVE"] as const;

export const LogicSelector = () => {
  const { t } = useTranslation("clients");
  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("logic")}
      labelIcon={
        <HelpItem
          helpText={t("clients-help:logic")}
          fieldLabelId="clients:logic"
        />
      }
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
                className="pf-u-mb-md"
              />
            ))}
          </>
        )}
      />
    </FormGroup>
  );
};
