import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useFieldArray, useForm } from "react-hook-form";
import {
  AlertVariant,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";

import { useAlerts } from "../components/alert/Alerts";
import {
  arrayToAttributes,
  AttributeForm,
  AttributesForm,
  attributesToArray,
} from "../components/attribute-form/AttributeForm";
import { useAdminClient } from "../context/auth/AdminClient";

import { getLastId } from "./groupIdUtils";
import { useSubGroups } from "./SubGroupsContext";
import { useLocation } from "react-router-dom";

export const GroupAttributes = () => {
  const { t } = useTranslation("groups");
  const adminClient = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const form = useForm<AttributeForm>({ mode: "onChange" });
  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "attributes",
  });

  const location = useLocation();
  const id = getLastId(location.pathname);
  const { currentGroup, subGroups, setSubGroups } = useSubGroups();

  const convertAttributes = (attr?: Record<string, any>) => {
    const attributes = attributesToArray(attr || currentGroup().attributes!);
    attributes.push({ key: "", value: "" });
    return attributes;
  };

  useEffect(() => {
    form.setValue("attributes", convertAttributes());
  }, [subGroups]);

  const save = async (attributeForm: AttributeForm) => {
    try {
      const group = currentGroup();
      const attributes = arrayToAttributes(attributeForm.attributes!);
      await adminClient.groups.update({ id: id! }, { ...group, attributes });

      setSubGroups([
        ...subGroups.slice(0, subGroups.length - 1),
        { ...group, attributes },
      ]);
      form.setValue("attributes", convertAttributes(attributes));
      addAlert(t("groupUpdated"), AlertVariant.success);
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
