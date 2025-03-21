import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
import type { ComponentProps } from "./components";

export const BooleanComponent = ({
  name,
  label,
  helpText,
  isDisabled = false,
  defaultValue,
  isNew = true,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  return (
    <FormGroup
      hasNoPaddingTop
      label={t(label!)}
      fieldId={name!}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
    >
      <Controller
        name={convertToName(name!)}
        data-testid={name}
        defaultValue={isNew ? defaultValue : false}
        control={control}
        render={({ field }) => (
          <Switch
            id={name!}
            isDisabled={isDisabled}
            label={t("on")}
            labelOff={t("off")}
            isChecked={
              field.value === "true" ||
              field.value === true ||
              field.value?.[0] === "true"
            }
            onChange={(_event, value) => field.onChange("" + value)}
            data-testid={name}
            aria-label={t(label!)}
          />
        )}
      />
    </FormGroup>
  );
};
