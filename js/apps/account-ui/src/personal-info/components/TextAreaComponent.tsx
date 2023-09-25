import { useFormContext } from "react-hook-form";
import { UserProfileAttributeMetadata } from "../../api/representations";
import { fieldName } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";
import { KeycloakTextArea } from "ui-shared";

export const TextAreaComponent = (attr: UserProfileAttributeMetadata) => {
  const { register } = useFormContext();

  return (
    <UserProfileGroup {...attr}>
      <KeycloakTextArea
        id={attr.name}
        data-testid={attr.name}
        {...register(fieldName(attr))}
        cols={attr.annotations?.["inputTypeCols"] as number}
        rows={attr.annotations?.["inputTypeRows"] as number}
      />
    </UserProfileGroup>
  );
};
