import {
  FormGroup,
  MenuToggle,
  Select,
  SelectList,
  SelectOption,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "@keycloak/keycloak-ui-shared";
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
      data-testid={`token-lifespan-${id}`}
    >
      <Controller
        name={name}
        defaultValue=""
        control={control}
        render={({ field }) => (
          <Split hasGutter>
            <SplitItem>
              <Select
                toggle={(ref) => (
                  <MenuToggle
                    ref={ref}
                    onClick={() => setOpen(!open)}
                    isExpanded={open}
                  >
                    {isExpireSet(field.value) ? t(expires) : t(inherited)}
                  </MenuToggle>
                )}
                isOpen={open}
                onOpenChange={(isOpen) => setOpen(isOpen)}
                onSelect={(_, value) => {
                  field.onChange(value);
                  setOpen(false);
                }}
                selected={isExpireSet(field.value) ? t(expires) : t(inherited)}
              >
                <SelectList>
                  <SelectOption value="">{t(inherited)}</SelectOption>
                  <SelectOption value={60}>{t(expires)}</SelectOption>
                </SelectList>
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
