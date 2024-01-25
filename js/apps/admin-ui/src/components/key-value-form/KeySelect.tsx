import { Grid, GridItem, Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { UseControllerProps, useController } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeycloakTextInput } from "ui-shared";
import useToggle from "../../utils/useToggle";
import { DefaultValue } from "./KeyValueInput";

type KeySelectProp = UseControllerProps & {
  selectItems: DefaultValue[];
};

export const KeySelect = ({ selectItems, ...rest }: KeySelectProp) => {
  const { t } = useTranslation();
  const [open, toggle] = useToggle();
  const { field } = useController(rest);
  const [custom, setCustom] = useState(
    !selectItems.map(({ key }) => key).includes(field.value),
  );

  return (
    <Grid>
      <GridItem lg={custom ? 2 : 12}>
        <Select
          onToggle={() => toggle()}
          isOpen={open}
          onSelect={(_, value) => {
            if (value) {
              setCustom(false);
            }
            field.onChange(value);
            toggle();
          }}
          selections={!custom ? [field.value] : ""}
        >
          {[
            <SelectOption key="custom" onClick={() => setCustom(true)}>
              {t("customAttribute")}
            </SelectOption>,
            ...selectItems.map((item) => (
              <SelectOption key={item.key} value={item.key}>
                {item.label}
              </SelectOption>
            )),
          ]}
        </Select>
      </GridItem>
      {custom && (
        <GridItem lg={10}>
          <KeycloakTextInput
            id="customValue"
            data-testid={rest.name}
            placeholder={t("keyPlaceholder")}
            value={field.value}
            onChange={field.onChange}
            autoFocus
          />
        </GridItem>
      )}
    </Grid>
  );
};
