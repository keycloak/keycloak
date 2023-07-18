import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  AlertVariant,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import {
  arrayToKeyValue,
  keyValueToArray,
} from "../components/key-value-form/key-value-convert";
import { useUserProfile } from "../realm-settings/user-profile/UserProfileContext";

type UserAttributesProps = {
  user: UserRepresentation;
};

export const UserAttributes = ({ user: defaultUser }: UserAttributesProps) => {
  const { t } = useTranslation("users");
  const { addAlert, addError } = useAlerts();
  const [user, setUser] = useState<UserRepresentation>(defaultUser);
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const { config } = useUserProfile();

  const convertAttributes = () => {
    return arrayToKeyValue<UserRepresentation>(user.attributes!);
  };

  useEffect(() => {
    form.setValue("attributes", convertAttributes());
  }, [user, config]);

  const save = async (attributeForm: AttributeForm) => {
    try {
      const attributes = keyValueToArray(attributeForm.attributes!);
      await adminClient.users.update({ id: user.id! }, { ...user, attributes });

      setUser({ ...user, attributes });
      addAlert(t("userSaved"), AlertVariant.success);
    } catch (error) {
      addError("groups:groupUpdateError", error);
    }
  };

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
