import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useFieldArray, useForm } from "react-hook-form";
import {
  AlertVariant,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";

import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";

import { useAlerts } from "../components/alert/Alerts";
import {
  arrayToAttributes,
  AttributeForm,
  AttributesForm,
  attributesToArray,
} from "../components/attribute-form/AttributeForm";
import { useAdminClient } from "../context/auth/AdminClient";

type UserAttributesProps = {
  user: UserRepresentation;
};

export const UserAttributes = ({ user }: UserAttributesProps) => {
  const { t } = useTranslation("users");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "attributes",
  });

  const convertAttributes = (attr?: Record<string, any>) => {
    const attributes = attributesToArray(attr || user.attributes!);
    attributes.push({ key: "", value: "" });
    return attributes;
  };

  useEffect(() => {
    form.setValue("attributes", convertAttributes());
  }, [user]);

  const save = async (attributeForm: AttributeForm) => {
    try {
      const attributes = arrayToAttributes(attributeForm.attributes);
      await adminClient.users.update({ id: user.id! }, { ...user, attributes });

      form.setValue("attributes", convertAttributes(attributes));
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
        array={{ fields, append, remove }}
        reset={() =>
          form.reset({
            attributes: convertAttributes(),
          })
        }
      />
    </PageSection>
  );
};
