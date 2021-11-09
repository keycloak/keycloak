import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { HelpItem } from "../help-enabler/HelpItem";
import type { ComponentProps } from "./components";
import { convertToHyphens } from "../../util";

export const MultiValuedListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
  selectedValues,
  parentCallback,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);
  const [selectedItems, setSelectedItems] = useState<string[]>([defaultValue]);

  useEffect(() => {
    if (selectedValues) {
      parentCallback!(selectedValues);
      setSelectedItems(selectedValues!);
    }
  }, []);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} forLabel={label!} forID={name!} />
      }
      fieldId={name!}
    >
      <Controller
        name={`config.${convertToHyphens(name!)}`}
        defaultValue={defaultValue ? [defaultValue] : []}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId={name}
            data-testid={name}
            chipGroupProps={{
              numChips: 3,
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
                parentCallback!([option]);
                setSelectedItems([option]);
              } else if (value.includes(option)) {
                const updatedItems = selectedItems.filter(
                  (item: string) => item !== option
                );
                setSelectedItems(updatedItems);
                onChange(updatedItems);
                parentCallback!(updatedItems);
              } else {
                onChange([...value, option]);
                parentCallback!([...value, option]);
                setSelectedItems([...selectedItems, option]);
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
