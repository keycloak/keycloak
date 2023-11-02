import { FormGroup, ValidatedOptions } from "@patternfly/react-core";
import { CSSProperties } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { TimeSelector } from "../../components/time-selector/TimeSelector";

export const Time = ({
  name,
  style,
}: {
  name: string;
  style?: CSSProperties;
}) => {
  const { t } = useTranslation();
  const {
    control,
    formState: { errors },
  } = useFormContext();
  return (
    <FormGroup
      style={style}
      label={t(name)}
      fieldId={name}
      labelIcon={<HelpItem helpText={t(`${name}Help`)} fieldLabelId={name} />}
      validated={
        errors[name] ? ValidatedOptions.error : ValidatedOptions.default
      }
      helperTextInvalid={t("required")}
    >
      <Controller
        name={name}
        defaultValue=""
        control={control}
        rules={{ required: true }}
        render={({ field }) => (
          <TimeSelector
            data-testid={name}
            value={field.value}
            onChange={field.onChange}
            validated={
              errors[name] ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        )}
      />
    </FormGroup>
  );
};
