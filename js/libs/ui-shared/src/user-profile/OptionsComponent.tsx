import { Checkbox, Radio } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { Options } from "./UserProfileFields";
import { UserProfileFieldsProps, UserProfileGroup } from "./UserProfileGroup";
import { fieldName } from "./utils";

export const OptionComponent = (attr: UserProfileFieldsProps) => {
  const { control } = useFormContext();
  const type = attr.annotations?.["inputType"] as string;
  const isMultiSelect = type.includes("multiselect");
  const Component = isMultiSelect ? Checkbox : Radio;

  const options = (attr.validators?.options as Options).options || [];

  return (
    <UserProfileGroup {...attr}>
      <Controller
        name={fieldName(attr)}
        control={control}
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
                readOnly={attr.readOnly}
              />
            ))}
          </>
        )}
      />
    </UserProfileGroup>
  );
};
