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
import { camelCase } from "lodash";
import { convertToHyphens } from "../../../util";

export const MultivaluedListComponent = ({
  name,
  label,
  helpText,
  defaultValue,
  options,
}: ComponentProps) => {
  const { t } = useTranslation("realm-settings-help");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);
  const [selectedItems, setSelectedItems] = useState<string[]>([defaultValue]);

  const convertToString = (s: string) =>
    camelCase(convertToHyphens(s).replaceAll("-", " "));

  return (
    <FormGroup
      label={t(
        `realm-settings:${convertToString(label!).replace("Label", "")}`
      )}
      labelIcon={
        <HelpItem
          helpText={
            name === "update-client-source"
              ? t(helpText!)
              : t(convertToString(helpText!))
          }
          forLabel={t(label!)}
          forID={name!}
        />
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
              numChips: 3,
              expandedText: t("common:hide"),
              collapsedText: t("common:showRemaining"),
            }}
            variant={SelectVariant.typeaheadMulti}
            typeAheadAriaLabel={t("common:select")}
            onToggle={(isOpen) => setOpen(isOpen)}
            selections={selectedItems}
            onSelect={(_, v) => {
              const option = v as string;
              if (!value) {
                setSelectedItems([option]);
                onChange([option]);
              } else if (selectedItems.includes(option)) {
                setSelectedItems(
                  selectedItems.filter((item: string) => item !== option)
                );
                onChange(selectedItems);
              } else {
                setSelectedItems([...selectedItems, option]);
                onChange([...selectedItems, option]);
              }
            }}
            onClear={(event) => {
              event.stopPropagation();
              onChange([]);
              setSelectedItems([]);
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
