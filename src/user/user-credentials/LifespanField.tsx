import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup } from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { credResetFormDefaultValues } from "./ResetCredentialDialog";

export const LifespanField = () => {
  const { t } = useTranslation("users");
  const { control } = useFormContext();

  return (
    <FormGroup
      fieldId="lifespan"
      label={t("lifespan")}
      isStack
      labelIcon={
        <HelpItem helpText="clients-help:lifespan" fieldLabelId="lifespan" />
      }
    >
      <Controller
        name="lifespan"
        defaultValue={credResetFormDefaultValues.lifespan}
        control={control}
        render={({ onChange, value }) => (
          <TimeSelector
            value={value}
            units={["minute", "hour", "day"]}
            onChange={onChange}
            menuAppendTo="parent"
          />
        )}
      />
    </FormGroup>
  );
};
