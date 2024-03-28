import { FormGroup, Split, SplitItem } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
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
const expires = "tokenLifespan.expires";

export const TokenLifespan = ({
  id,
  name,
  defaultValue,
  units,
}: TokenLifespanProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const [focused, setFocused] = useState(false);
  const onFocus = () => setFocused(true);
  const onBlur = () => setFocused(false);

  const { control } = useFormContext();
  const isExpireSet = (value: string | number) =>
    typeof value === "number" ||
    (typeof value === "string" && value !== "") ||
    focused;

  return (
    <FormGroup
      label={t(id)}
      fieldId={id}
      labelIcon={<HelpItem helpText={t(`${id}Help`)} fieldLabelId={id} />}
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
                onToggle={(_event, val) => setOpen(val)}
                isOpen={open}
                onSelect={(_, value) => {
                  field.onChange(value);
                  setOpen(false);
                }}
                selections={[
                  isExpireSet(field.value) ? t(expires) : t(inherited),
                ]}
              >
                <SelectOption value="">{t(inherited)}</SelectOption>
                <SelectOption value={60}>{t(expires)}</SelectOption>
              </Select>
            </SplitItem>
            <SplitItem hidden={!isExpireSet(field.value)}>
              <TimeSelector
                validated={
                  isExpireSet(field.value) && field.value! < 1
                    ? "warning"
                    : "default"
                }
                units={units}
                value={field.value === "" ? defaultValue : field.value}
                onChange={field.onChange}
                onFocus={onFocus}
                onBlur={onBlur}
                min={1}
                isDisabled={!isExpireSet(field.value)}
              />
            </SplitItem>
          </Split>
        )}
      />
    </FormGroup>
  );
};
