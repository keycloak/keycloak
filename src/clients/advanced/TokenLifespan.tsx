import React, { useState } from "react";
import { Control, Controller, FieldValues } from "react-hook-form";
import { useTranslation } from "react-i18next";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
} from "@patternfly/react-core";

import {
  TimeSelector,
  Unit,
} from "../../components/time-selector/TimeSelector";
import { HelpItem } from "../../components/help-enabler/HelpItem";

type TokenLifespanProps = {
  id: string;
  name: string;
  defaultValue: string;
  control: Control<FieldValues>;
  units?: Unit[];
};

const never = "tokenLifespan.never";
const expires = "tokenLifespan.expires";

export const TokenLifespan = ({
  id,
  name,
  defaultValue,
  control,
  units,
}: TokenLifespanProps) => {
  const { t } = useTranslation("clients");
  const [open, setOpen] = useState(false);

  const [focused, setFocused] = useState(false);
  const onFocus = () => setFocused(true);
  const onBlur = () => setFocused(false);

  const isExpireSet = (value: string | number) =>
    (typeof value === "number" && value !== -1) || focused;

  return (
    <FormGroup
      label={t(id)}
      fieldId={id}
      labelIcon={
        <HelpItem
          helpText={`clients-help:${id}`}
          fieldLabelId={`clients:${id}`}
        />
      }
    >
      <Controller
        name={name}
        defaultValue={defaultValue}
        control={control}
        render={({ onChange, value }) => (
          <Split hasGutter>
            <SplitItem>
              <Select
                variant={SelectVariant.single}
                onToggle={setOpen}
                isOpen={open}
                onSelect={(_, value) => {
                  onChange(value);
                  setOpen(false);
                }}
                selections={[isExpireSet(value) ? t(expires) : t(never)]}
              >
                <SelectOption value={-1}>{t(never)}</SelectOption>
                <SelectOption value={60}>{t(expires)}</SelectOption>
              </Select>
            </SplitItem>
            <SplitItem>
              {isExpireSet(value) && (
                <TimeSelector
                  units={units}
                  value={value}
                  onChange={onChange}
                  onFocus={onFocus}
                  onBlur={onBlur}
                  min={1}
                />
              )}
            </SplitItem>
          </Split>
        )}
      />
    </FormGroup>
  );
};
