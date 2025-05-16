import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { PageSection } from "@patternfly/react-core";
import { UseFormReturn, useFormContext } from "react-hook-form";

import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import { UserFormFields, toUserFormFields } from "./form-state";
import {
  UnmanagedAttributePolicy,
  UserProfileConfig,
} from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";

type UserAttributesProps = {
  user: UserRepresentation;
  save: (user: UserFormFields) => void;
  upConfig?: UserProfileConfig;
};

export const UserAttributes = ({
  user,
  save,
  upConfig,
}: UserAttributesProps) => {
  const form = useFormContext<UserFormFields>();

  return (
    <PageSection hasBodyWrapper={false}>
      <AttributesForm
        form={form as UseFormReturn<AttributeForm>}
        save={save}
        fineGrainedAccess={user.access?.manage}
        reset={() =>
          form.reset({
            ...form.getValues(),
            attributes: toUserFormFields(user).attributes,
          })
        }
        name="unmanagedAttributes"
        isDisabled={
          UnmanagedAttributePolicy.AdminView ==
          upConfig?.unmanagedAttributePolicy
        }
      />
    </PageSection>
  );
};
