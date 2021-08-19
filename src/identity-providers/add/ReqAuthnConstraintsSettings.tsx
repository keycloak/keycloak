import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { TextField } from "../component/TextField";
import { HelpItem } from "../../components/help-enabler/HelpItem";

const comparisonValues = ["Exact", "Minimum", "Maximum", "Better"];

export const ReqAuthnConstraints = () => {
  const { t } = useTranslation("identity-providers");
  const { control } = useFormContext();
  const [syncModeOpen, setSyncModeOpen] = useState(false);
  return (
    <>
      <FormGroup
        label={t("comparison")}
        labelIcon={
          <HelpItem
            helpText="identity-providers-help:comparison"
            forLabel={t("comparison")}
            forID="comparison"
          />
        }
        fieldId="comparison"
      >
        <Controller
          name="config.comparison"
          defaultValue={comparisonValues[0]}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="comparison"
              required
              direction="up"
              onToggle={() => setSyncModeOpen(!syncModeOpen)}
              onSelect={(_, value) => {
                onChange(value.toString());
                setSyncModeOpen(false);
              }}
              selections={value}
              variant={SelectVariant.single}
              aria-label={t("syncMode")}
              isOpen={syncModeOpen}
            >
              {comparisonValues.map((option) => (
                <SelectOption
                  selected={option === value}
                  key={option}
                  value={option}
                >
                  {t(option)}
                </SelectOption>
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <TextField
        field="config.authnContextClassRefs"
        label="authnContextClassRefs"
      />
      <TextField
        field="config.authnContextDeclRefs"
        label="authnContextDeclRefs"
      />
    </>
  );
};
