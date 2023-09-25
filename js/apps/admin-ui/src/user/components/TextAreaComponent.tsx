import { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { useFormContext } from "react-hook-form";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { UserProfileGroup } from "./UserProfileGroup";
import { fieldName } from "../utils";

export const TextAreaComponent = (attr: UserProfileAttribute) => {
  const { register } = useFormContext();

  return (
    <UserProfileGroup {...attr}>
      <KeycloakTextArea
        id={attr.name}
        data-testid={attr.name}
        {...register(fieldName(attr))}
        cols={attr.annotations?.["inputTypeCols"] as number}
        rows={attr.annotations?.["inputTypeRows"] as number}
        readOnly={attr.readOnly}
      />
    </UserProfileGroup>
  );
};
