import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import {
  TimeSelector,
  Unit,
} from "../../components/time-selector/TimeSelector";

type TokenLifespanProps = {
  id: string;
  name: string;
  defaultValue?: number;
  units?: Unit[];
};

const inherited = "tokenLifespan.inherited";
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
          helpText={t(`clients-help:${id}`)}
          fieldLabelId={`clients:${id}`}
        />
      }
    >
      <Controller
        name={name}
        defaultValue=""
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
                selections={[
                  isExpireSet(field.value)
                    ? t(expires)
                    : field.value === ""
                    ? t(inherited)
                    : t(never),
                ]}
              >
                <SelectOption value="">{t(inherited)}</SelectOption>
                <SelectOption value={-1}>{t(never)}</SelectOption>
                <SelectOption value={60}>{t(expires)}</SelectOption>
              </Select>
            </SplitItem>
            <SplitItem>
              {field.value !== "-1" && field.value !== -1 && (
                <TimeSelector
                  units={units}
                  value={field.value === "" ? defaultValue : field.value}
                  onChange={field.onChange}
                  onFocus={onFocus}
                  onBlur={onBlur}
                  min={1}
                  isDisabled={field.value === ""}
                />
              )}
            </SplitItem>
          </Split>
        )}
      />
    </FormGroup>
  );
};
