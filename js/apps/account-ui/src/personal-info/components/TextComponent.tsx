import { TextInput } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";

import { UserProfileAttributeMetadata } from "../../api/representations";
import { fieldName } from "../utils";
import { UserProfileGroup } from "./UserProfileGroup";

export const TextComponent = (attr: UserProfileAttributeMetadata) => {
  const { register } = useFormContext();
  const inputType = attr.annotations?.["inputType"] as string | undefined;
  const type: any = inputType?.startsWith("html")
    ? inputType.substring("html".length + 2)
    : "text";

  return (
    <UserProfileGroup {...attr}>
      <TextInput
        id={attr.name}
        data-testid={attr.name}
        type={type}
        placeholder={attr.annotations?.["inputTypePlaceholder"] as string}
        {...register(fieldName(attr))}
      />
    </UserProfileGroup>
  );
};
