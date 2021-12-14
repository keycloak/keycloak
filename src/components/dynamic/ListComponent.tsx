import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import type { ComponentProps } from "./components";
import { HelpItem } from "../help-enabler/HelpItem";

export const ListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <Controller
        name={`config.${name}`}
        data-testid={name}
        defaultValue={defaultValue || ""}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId={name}
            onToggle={(toggle) => setOpen(toggle)}
            onSelect={(_, value) => {
              onChange(value as string);
              setOpen(false);
            }}
            selections={value}
            variant={SelectVariant.single}
            aria-label={t(label!)}
            isOpen={open}
          >
            {options?.map((option) => (
              <SelectOption
                selected={option === value}
                key={option}
                value={option}
              />
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
