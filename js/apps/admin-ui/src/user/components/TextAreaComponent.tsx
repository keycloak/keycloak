import { FieldPath } from "react-hook-form";

import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { UserProfileFieldProps } from "../UserProfileFields";
import { UserFormFields } from "../form-state";
import { fieldName } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";

export const TextAreaComponent = ({
  form,
  attribute,
}: UserProfileFieldProps) => {
  return (
    <UserProfileGroup form={form} attribute={attribute}>
      <KeycloakTextArea
        id={attribute.name}
        data-testid={attribute.name}
        {...form.register(fieldName(attribute) as FieldPath<UserFormFields>)}
        cols={attribute.annotations?.["inputTypeCols"] as number}
        rows={attribute.annotations?.["inputTypeRows"] as number}
        readOnly={attribute.readOnly}
      />
    </UserProfileGroup>
  );
};
