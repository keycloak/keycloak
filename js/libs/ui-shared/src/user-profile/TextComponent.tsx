import { useFormContext } from "react-hook-form";
import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";
import { UserProfileFieldsProps, UserProfileGroup } from "./UserProfileGroup";
import { fieldName } from "./utils";

export const TextComponent = (attr: UserProfileFieldsProps) => {
  const { register } = useFormContext();
  const inputType = attr.annotations?.["inputType"] as string | undefined;
  const type: any = inputType?.startsWith("html")
    ? inputType.substring("html".length + 2)
    : "text";

  return (
    <UserProfileGroup {...attr}>
      <KeycloakTextInput
        id={attr.name}
        data-testid={attr.name}
        type={type}
        placeholder={attr.annotations?.["inputTypePlaceholder"] as string}
        readOnly={attr.readOnly}
        {...register(fieldName(attr))}
      />
    </UserProfileGroup>
  );
};
