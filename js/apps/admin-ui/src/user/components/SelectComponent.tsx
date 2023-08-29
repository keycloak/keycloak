import { Select, SelectOption } from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { Options } from "../UserProfileFields";
import { fieldName } from "../utils";
import { UserProfileFieldsProps, UserProfileGroup } from "./UserProfileGroup";

export const SelectComponent = (attribute: UserProfileFieldsProps) => {
  const { t } = useTranslation("users");
  const { control } = useFormContext();
  const [open, setOpen] = useState(false);

  const isMultiSelect = attribute.annotations?.["inputType"] === "multiselect";
  const options = (attribute.validations?.options as Options).options || [];
  return (
    <UserProfileGroup {...attribute}>
      <Controller
        name={fieldName(attribute)}
        defaultValue=""
        control={control}
        render={({ field }) => (
          <Select
            toggleId={attribute.name}
            onToggle={(b) => setOpen(b)}
            onSelect={(_, value) => {
              const option = value.toString();
              if (isMultiSelect) {
                if (field.value.includes(option)) {
                  field.onChange(
                    field.value.filter((item: string) => item !== option),
                  );
                } else {
                  field.onChange([...field.value, option]);
                }
              } else {
                field.onChange(option);
                setOpen(false);
              }
            }}
            selections={field.value ? field.value : t("common:choose")}
            variant={isMultiSelect ? "typeaheadmulti" : "single"}
            aria-label={t("common:selectOne")}
            isOpen={open}
            readOnly={attribute.readOnly}
          >
            {options.map((option) => (
              <SelectOption
                selected={field.value === option}
                key={option}
                value={option}
              >
                {option}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </UserProfileGroup>
  );
};
