import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { HelpItem } from "../../../components/help-enabler/HelpItem";
import type { ComponentProps } from "./components";

export const MultivaluedListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
}: ComponentProps) => {
  const { t } = useTranslation("client-scopes");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} forLabel={t(label!)} forID={name!} />
      }
      fieldId={name!}
    >
      <Controller
        name={label!}
        defaultValue={defaultValue || []}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId={name}
            data-testid={name}
            chipGroupProps={{
              numChips: 1,
              expandedText: t("common:hide"),
              collapsedText: t("common:showRemaining"),
            }}
            variant={SelectVariant.typeaheadMulti}
            typeAheadAriaLabel={t("common:select")}
            onToggle={(isOpen) => setOpen(isOpen)}
            selections={value}
            onSelect={(_, v) => {
              const option = v.toString();
              if (!value) {
                onChange([option]);
              } else if (value.includes(option)) {
                onChange(value.filter((item: string) => item !== option));
              } else {
                onChange([...value, option]);
              }
            }}
            onClear={(event) => {
              event.stopPropagation();
              onChange([]);
            }}
            isOpen={open}
            aria-label={t(label!)}
          >
            {options?.map((option) => (
              <SelectOption key={option} value={option} />
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
