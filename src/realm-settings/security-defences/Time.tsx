import React, { CSSProperties } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, ValidatedOptions } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { TimeSelector } from "../../components/time-selector/TimeSelector";

export const Time = ({
  name,
  style,
}: {
  name: string;
  style?: CSSProperties;
}) => {
  const { t } = useTranslation("realm-settings");
  const { control, errors } = useFormContext();
  return (
    <FormGroup
      style={style}
      label={t(name)}
      fieldId={name}
      labelIcon={
        <HelpItem
          helpText={`realm-settings-help:${name}`}
          fieldLabelId={`realm-settings:${name}`}
        />
      }
      validated={
        errors[name] ? ValidatedOptions.error : ValidatedOptions.default
      }
      helperTextInvalid={t("common:required")}
    >
      <Controller
        name={name}
        defaultValue=""
        control={control}
        rules={{ required: true }}
        render={({ onChange, value }) => (
          <TimeSelector
            data-testid={name}
            value={value}
            onChange={onChange}
            validated={
              errors[name] ? ValidatedOptions.error : ValidatedOptions.default
            }
          />
        )}
      />
    </FormGroup>
  );
};
