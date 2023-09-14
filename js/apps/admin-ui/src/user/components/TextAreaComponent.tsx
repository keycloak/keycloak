import { UserProfileAttribute } from "@keycloak/keycloak-admin-client/lib/defs/userProfileConfig";
import { TextArea } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";

import { fieldName } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";

export const TextAreaComponent = (attr: UserProfileAttribute) => {
  const { register } = useFormContext();

  return (
    <UserProfileGroup {...attr}>
      <TextArea
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
