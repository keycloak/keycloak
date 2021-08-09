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

const types = ["basic-flow", "client-flow"];

export const FlowType = () => {
  const { t } = useTranslation("authentication");
  const { control } = useFormContext();

  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t("flowType")}
      labelIcon={
        <HelpItem
          helpText="authentication-help:flowType"
          forLabel={t("flowType")}
          forID="flowType"
        />
      }
      fieldId="flowType"
    >
      <Controller
        name="providerId"
        defaultValue={types[0]}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId="flowType"
            onToggle={() => setOpen(!open)}
            onSelect={(_, value) => {
              onChange(value as string);
              setOpen(false);
            }}
            selections={t(`flow-type.${value}`)}
            variant={SelectVariant.single}
            aria-label={t("flowType")}
            isOpen={open}
          >
            {types.map((type) => (
              <SelectOption
                selected={type === value}
                key={type}
                value={t(`flow-type.${type}`)}
              />
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
