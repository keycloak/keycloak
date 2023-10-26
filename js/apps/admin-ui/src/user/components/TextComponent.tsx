import { TextInputTypes } from "@patternfly/react-core";
import { FieldPath } from "react-hook-form";
import { KeycloakTextInput } from "ui-shared";

import { UserProfileFieldProps } from "../UserProfileFields";
import { UserFormFields } from "../form-state";
import { fieldName } from "../utils";
import { isRequiredAttribute } from "../utils/user-profile";
import { UserProfileGroup } from "./UserProfileGroup";

export const TextComponent = ({
  form,
  inputType,
  attribute,
}: UserProfileFieldProps) => {
  const isRequired = isRequiredAttribute(attribute);
  const type = inputType.startsWith("html")
    ? (inputType.substring("html".length + 2) as TextInputTypes)
    : "text";

  return (
    <UserProfileGroup form={form} attribute={attribute}>
      <KeycloakTextInput
        id={attribute.name}
        data-testid={attribute.name}
        type={type}
        placeholder={attribute.annotations?.["inputTypePlaceholder"] as string}
        readOnly={attribute.readOnly}
        isRequired={isRequired}
        {...form.register(fieldName(attribute) as FieldPath<UserFormFields>)}
      />
    </UserProfileGroup>
  );
};
