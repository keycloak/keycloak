import { FormGroup, TextInput } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";

export const DisplayOrder = () => {
  const { t } = useTranslation();

  const { control } = useFormContext();

  return (
    <FormGroup
      label={t("displayOrder")}
      labelIcon={
        <HelpItem
          helpText={t("displayOrderHelp")}
          fieldLabelId="displayOrder"
        />
      }
      fieldId="kc-display-order"
    >
      <Controller
        name="config.guiOrder"
        control={control}
        defaultValue=""
        render={({ field }) => (
          <TextInput
            id="kc-display-order"
            type="number"
            value={field.value}
            data-testid="displayOrder"
            min={0}
            onChange={(_event, value) => {
              const num = Number(value);
              field.onChange(value === "" ? value : num < 0 ? 0 : num);
            }}
          />
        )}
      />
    </FormGroup>
  );
};
