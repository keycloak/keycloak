import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, TextInput } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";

export const DisplayOrder = () => {
  const { t } = useTranslation("identity-providers");

  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("displayOrder")}
      labelIcon={
        <HelpItem
          helpText="identity-providers-help:displayOrder"
          fieldLabelId="identity-providers:displayOrder"
        />
      }
      fieldId="kc-display-order"
    >
      <Controller
        name="config.guiOrder"
        control={control}
        defaultValue=""
        render={({ onChange, value }) => (
          <TextInput
            id="kc-display-order"
            type="number"
            value={value}
            data-testid="displayOrder"
            min={0}
            onChange={(value) => {
              const num = Number(value);
              onChange(value === "" ? value : num < 0 ? 0 : num);
            }}
          />
        )}
      />
    </FormGroup>
  );
};
