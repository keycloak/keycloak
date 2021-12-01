import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import {
  AlertVariant,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

import { useAlerts } from "../components/alert/Alerts";
import {
  AttributeForm,
  AttributesForm,
} from "../components/attribute-form/AttributeForm";
import {
  attributesToArray,
  arrayToAttributes,
} from "../components/attribute-form/attribute-convert";
import { useAdminClient } from "../context/auth/AdminClient";

type UserAttributesProps = {
  user: UserRepresentation;
};

export const UserAttributes = ({ user: defaultUser }: UserAttributesProps) => {
  const { t } = useTranslation("users");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const [user, setUser] = useState<UserRepresentation>(defaultUser);
  const form = useForm<AttributeForm>({ mode: "onChange" });

  const convertAttributes = (attr?: Record<string, any>) => {
    return attributesToArray(attr || user.attributes!);
  };

  useEffect(() => {
    form.setValue("attributes", convertAttributes());
  }, [user]);

  const save = async (attributeForm: AttributeForm) => {
    try {
      const attributes = arrayToAttributes(attributeForm.attributes!);
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
        reset={() =>
          form.reset({
            attributes: convertAttributes(),
          })
        }
      />
    </PageSection>
  );
};
