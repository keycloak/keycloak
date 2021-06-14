import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, NumberInput } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";

export const DisplayOrder = () => {
  const { t } = useTranslation("identity-providers");
  const { t: th } = useTranslation("identity-providers-help");

  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("displayOrder")}
      labelIcon={
        <HelpItem
          helpText={th("displayOrder")}
          forLabel={t("displayOrder")}
          forID="kc-display-order"
        />
      }
      fieldId="kc-display-order"
    >
      <Controller
        name="config.guiOrder"
        control={control}
        defaultValue={0}
        render={({ onChange, value }) => (
          <NumberInput
            value={value}
            data-testid="displayOrder"
            min={0}
            onMinus={() => onChange(Number.parseInt(value) - 1)}
            onChange={onChange}
            onPlus={() => onChange(Number.parseInt(value) + 1)}
            inputName="input"
            inputAriaLabel={t("displayOrder")}
            minusBtnAriaLabel={t("common:minus")}
            plusBtnAriaLabel={t("common:plus")}
          />
        )}
      />
    </FormGroup>
  );
};
