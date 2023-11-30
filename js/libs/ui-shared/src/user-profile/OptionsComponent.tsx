import { Checkbox, Radio } from "@patternfly/react-core";
import { Controller } from "react-hook-form";
import { Options, UserProfileFieldProps } from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import { fieldName, isRequiredAttribute } from "./utils";

export const OptionComponent = (props: UserProfileFieldProps) => {
  const { form, inputType, attribute } = props;
  const isRequired = isRequiredAttribute(attribute);
  const isMultiSelect = inputType.startsWith("multiselect");
  const Component = isMultiSelect ? Checkbox : Radio;
  const options =
    (attribute.validators?.options as Options | undefined)?.options || [];

  return (
    <UserProfileGroup {...props}>
      <Controller
        name={fieldName(attribute.name)}
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
                isRequired={isRequired}
              />
            ))}
          </>
        )}
      />
    </UserProfileGroup>
  );
};
