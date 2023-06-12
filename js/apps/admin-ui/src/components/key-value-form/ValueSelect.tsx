import { Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { UseControllerProps, useController } from "react-hook-form";
import { useTranslation } from "react-i18next";

type ValueSelectProps = UseControllerProps & {
  selectItems: string[];
};

export const ValueSelect = ({ selectItems, ...rest }: ValueSelectProps) => {
  const { t } = useTranslation();
  const { field } = useController(rest);
  const [open, setOpen] = useState(false);

  return (
    <Select
      onToggle={(isOpen) => setOpen(isOpen)}
      isOpen={open}
      onSelect={(_, value) => {
        field.onChange(value);
        setOpen(false);
      }}
      selections={[field.value]}
      placeholder={t("valuePlaceholder")}
    >
      {selectItems.map((item) => (
        <SelectOption key={item} value={item} />
      ))}
    </Select>
  );
};
