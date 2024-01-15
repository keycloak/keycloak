import { Grid, GridItem, Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { UseControllerProps, useController } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeycloakTextInput } from "ui-shared";
import useToggle from "../../utils/useToggle";
import { DefaultValue } from "./KeyValueInput";

/**
 * Used when the values passed to KeySelect in "selectItems" need to be fetched from the server dynamically. In this case, we might want to call "setCustom"
 * from parent component. The "fieldValue" sent to KeySelect is not supposed to be set by the calling component, as it will be set by KeySelect itself with the value of the field, so
 * it can be consumed from the callback function once the values are fetched from the server
 */
export type FetchCallback = {
  custom: boolean;
  setCustom: any;
};

type KeySelectProp = UseControllerProps & {
  selectItems: DefaultValue[];
  fetchCallback?: FetchCallback;
};

export const KeySelect = ({
  selectItems,
  fetchCallback,
  ...rest
}: KeySelectProp) => {
  const { t } = useTranslation();
  const [open, toggle] = useToggle();
  const { field } = useController(rest);

  let [custom, setCustom] = useState(
    !selectItems.map(({ key }) => key).includes(field.value),
  );

  if (fetchCallback) {
    [custom, setCustom] = [fetchCallback.custom, fetchCallback.setCustom];
  }

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
