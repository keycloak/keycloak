import {
  TextInput,
  TextInputReadOnlyVariant,
  TextInputTypes,
} from "@patternfly/react-core";

import { UserProfileFieldProps } from "./UserProfileFields";
import { UserProfileGroup } from "./UserProfileGroup";
import { fieldName, isRequiredAttribute, label } from "./utils";

export const TextComponent = (props: UserProfileFieldProps) => {
  const { form, inputType, attribute } = props;
  const isRequired = isRequiredAttribute(attribute);
  const type = inputType.startsWith("html")
    ? (inputType.substring("html".length + 2) as TextInputTypes)
    : "text";

  return (
    <UserProfileGroup {...props}>
      <TextInput
        id={attribute.name}
        data-testid={attribute.name}
        type={type}
        placeholder={
          attribute.readOnly
            ? ""
            : label(
                props.t,
                attribute.annotations?.["inputTypePlaceholder"] as string,
                "",
                attribute.annotations?.[
                  "inputOptionLabelsI18nPrefix"
                ] as string,
              )
        }
        readOnlyVariant={
          attribute.readOnly ? TextInputReadOnlyVariant.default : undefined
        }
        isRequired={isRequired}
        {...form.register(fieldName(attribute.name))}
      />
    </UserProfileGroup>
  );
};
