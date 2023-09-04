import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { useFormContext } from "react-hook-form";

import { AttributesForm } from "../components/key-value-form/AttributeForm";
import { UserFormFields, toUserFormFields } from "./form-state";

type UserAttributesProps = {
  user: UserRepresentation;
  save: (user: UserFormFields) => void;
};

export const UserAttributes = ({ user, save }: UserAttributesProps) => {
  const form = useFormContext<UserFormFields>();

  return (
    <PageSection variant={PageSectionVariants.light}>
      <AttributesForm
        form={form}
        save={save}
        fineGrainedAccess={user.access?.manage}
        reset={() =>
          form.reset({
            ...form.getValues(),
            attributes: toUserFormFields(user).attributes,
          })
        }
      />
    </PageSection>
  );
};
