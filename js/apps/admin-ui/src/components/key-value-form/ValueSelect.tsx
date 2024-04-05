import { TextInput } from "@patternfly/react-core";
import { Select, SelectOption } from "@patternfly/react-core/deprecated";
import { useMemo, useState } from "react";
import { UseControllerProps, useController } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { DefaultValue } from "./KeyValueInput";

type ValueSelectProps = UseControllerProps & {
  selectItems: DefaultValue[];
  keyValue: string;
};

export const ValueSelect = ({
  selectItems,
  keyValue,
  ...rest
}: ValueSelectProps) => {
  const { t } = useTranslation();
  const { field } = useController(rest);
  const [open, setOpen] = useState(false);

  const defaultItem = useMemo(
    () => selectItems.find((v) => v.key === keyValue),
    [selectItems, keyValue],
  );

  return defaultItem?.values ? (
    <Select
      onToggle={(_event, isOpen) => setOpen(isOpen)}
      isOpen={open}
      onSelect={(_, value) => {
        field.onChange(value);
        setOpen(false);
      }}
      selections={field.value ? [field.value] : t("choose")}
      placeholder={t("valuePlaceholder")}
    >
      {defaultItem.values.map((item) => (
        <SelectOption key={item} value={item} />
      ))}
    </Select>
  ) : (
    <TextInput
      aria-label={t("customValue")}
      data-testid={rest.name}
      {...field}
    />
  );
};
