import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form-v7";
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
  units?: Unit[];
};

const never = "tokenLifespan.never";
const expires = "tokenLifespan.expires";

export const TokenLifespan = ({
  id,
  name,
  defaultValue,
  units,
}: TokenLifespanProps) => {
  const { t } = useTranslation("clients");
  const [open, setOpen] = useState(false);

  const [focused, setFocused] = useState(false);
  const onFocus = () => setFocused(true);
  const onBlur = () => setFocused(false);

  const { control } = useFormContext();
  const isExpireSet = (value: string | number) =>
    (typeof value === "number" && value !== -1) ||
    (typeof value === "string" && value !== "" && value !== "-1") ||
    focused;

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
        render={({ field }) => (
          <Split hasGutter>
            <SplitItem>
              <Select
                variant={SelectVariant.single}
                onToggle={setOpen}
                isOpen={open}
                onSelect={(_, value) => {
                  field.onChange(value);
                  setOpen(false);
                }}
                selections={[isExpireSet(field.value) ? t(expires) : t(never)]}
              >
                <SelectOption value={-1}>{t(never)}</SelectOption>
                <SelectOption value={60}>{t(expires)}</SelectOption>
              </Select>
            </SplitItem>
            <SplitItem>
              {isExpireSet(field.value) && (
                <TimeSelector
                  units={units}
                  value={field.value}
                  onChange={field.onChange}
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
