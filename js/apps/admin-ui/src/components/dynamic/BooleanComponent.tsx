import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import type { ComponentProps } from "./components";
import { convertToName } from "./DynamicComponents";

export const BooleanComponent = ({
  name,
  label,
  helpText,
  isDisabled = false,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();

  return (
    <FormGroup
      hasNoPaddingTop
      label={t(label!)}
      fieldId={name!}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
    >
      <Controller
        name={convertToName(name!)}
        data-testid={name}
        defaultValue={false}
        control={control}
        render={({ field }) => (
          <Switch
            id={name!}
            isDisabled={isDisabled}
            label={t("common:on")}
            labelOff={t("common:off")}
            isChecked={
              field.value === "true" ||
              field.value === true ||
              field.value[0] === "true"
            }
            onChange={(value) => field.onChange("" + value)}
            data-testid={name}
            aria-label={t(label!)}
          />
        )}
      />
    </FormGroup>
  );
};
