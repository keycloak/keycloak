import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";

const TYPES = ["basic-flow", "client-flow"] as const;

export const FlowType = () => {
  const { t } = useTranslation("authentication");
  const { control } = useFormContext();

  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t("flowType")}
      labelIcon={
        <HelpItem
          helpText="authentication-help:topLevelFlowType"
          fieldLabelId="authentication:flowType"
        />
      }
      fieldId="flowType"
    >
      <Controller
        name="providerId"
        defaultValue={TYPES[0]}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId="flowType"
            onToggle={setOpen}
            onSelect={(_, value) => {
              onChange(value.toString());
              setOpen(false);
            }}
            selections={t(`top-level-flow-type.${value}`)}
            variant={SelectVariant.single}
            aria-label={t("flowType")}
            isOpen={open}
          >
            {TYPES.map((type) => (
              <SelectOption selected={type === value} key={type} value={type}>
                {t(`top-level-flow-type.${type}`)}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
