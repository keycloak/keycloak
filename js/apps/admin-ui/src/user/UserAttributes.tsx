import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { useEffect } from "react";
import { useForm } from "react-hook-form";

import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import { arrayToKeyValue } from "../components/key-value-form/key-value-convert";
import { useUserProfile } from "../realm-settings/user-profile/UserProfileContext";

type UserAttributesProps = {
  user: UserRepresentation;
  save: (user: UserRepresentation) => void;
};

export const UserAttributes = ({ user, save }: UserAttributesProps) => {
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const { config } = useUserProfile();

  const convertAttributes = () => {
    return arrayToKeyValue<UserRepresentation>(user.attributes!);
  };

  useEffect(() => {
    form.setValue("attributes", convertAttributes());
  }, [user, config]);

  return (
    <PageSection variant={PageSectionVariants.light}>
      <AttributesForm
        form={form}
        save={save}
        fineGrainedAccess={user.access?.manage}
        reset={() =>
          form.reset({
            attributes: convertAttributes(),
          })
        }
      />
    </PageSection>
  );
};
