import { FormGroup } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
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
        <HelpItem
          helpText={t("clients-help:lifespan")}
          fieldLabelId="lifespan"
        />
      }
    >
      <Controller
        name="lifespan"
        defaultValue={credResetFormDefaultValues.lifespan}
        control={control}
        render={({ field }) => (
          <TimeSelector
            value={field.value}
            units={["minute", "hour", "day"]}
            onChange={field.onChange}
            menuAppendTo="parent"
          />
        )}
      />
    </FormGroup>
  );
};
