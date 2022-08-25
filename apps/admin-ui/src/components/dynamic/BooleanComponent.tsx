import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormGroup, Switch } from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "../help-enabler/HelpItem";
import { convertToName } from "./DynamicComponents";

export const BooleanComponent = ({
  name,
  label,
  helpText,
  defaultValue,
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
        defaultValue={defaultValue || false}
        control={control}
        render={({ onChange, value }) => (
          <Switch
            id={name!}
            isDisabled={isDisabled}
            label={t("common:on")}
            labelOff={t("common:off")}
            isChecked={value === "true" || value === true}
            onChange={(value) => onChange("" + value)}
            data-testid={name}
          />
        )}
      />
    </FormGroup>
  );
};
