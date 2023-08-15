import { useFormContext } from "react-hook-form";
import { KeycloakTextInput } from "ui-shared";
import { fieldName } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";
import { UserProfileAttributeMetadata } from "../../api/representations";

export const TextComponent = (attr: UserProfileAttributeMetadata) => {
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
        {...register(fieldName(attr))}
      />
    </UserProfileGroup>
  );
};
