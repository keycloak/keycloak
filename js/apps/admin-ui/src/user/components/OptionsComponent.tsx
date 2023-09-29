import { Checkbox, Radio } from "@patternfly/react-core";
import { Controller, FieldPath } from "react-hook-form";

import { Options, UserProfileFieldProps } from "../UserProfileFields";
import { UserFormFields } from "../form-state";
import { fieldName } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";

export const OptionComponent = ({
  form,
  inputType,
  attribute,
}: UserProfileFieldProps) => {
  const isMultiSelect = inputType.includes("multiselect");
  const Component = isMultiSelect ? Checkbox : Radio;
  const options = (attribute.validators?.options as Options).options || [];

  return (
    <UserProfileGroup form={form} attribute={attribute}>
      <Controller
        name={fieldName(attribute) as FieldPath<UserFormFields>}
        control={form.control}
        defaultValue=""
        render={({ field }) => (
          <>
            {options.map((option) => (
              <Component
                key={option}
                id={option}
                data-testid={option}
                label={option}
                value={option}
                isChecked={field.value.includes(option)}
                onChange={() => {
                  if (isMultiSelect) {
                    if (field.value.includes(option)) {
                      field.onChange(
                        field.value.filter((item: string) => item !== option),
                      );
                    } else {
                      field.onChange([...field.value, option]);
                    }
                  } else {
                    field.onChange([option]);
                  }
                }}
                readOnly={attribute.readOnly}
              />
            ))}
          </>
        )}
      />
    </UserProfileGroup>
  );
};
