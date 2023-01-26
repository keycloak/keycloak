import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
import {
  AlertVariant,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";

import { useAlerts } from "../components/alert/Alerts";
import {
  AttributeForm,
  AttributesForm,
} from "../components/key-value-form/AttributeForm";
import {
  keyValueToArray,
  arrayToKeyValue,
} from "../components/key-value-form/key-value-convert";
import { useAdminClient } from "../context/auth/AdminClient";

import { getLastId } from "./groupIdUtils";
import { useSubGroups } from "./SubGroupsContext";
import { useLocation } from "react-router-dom";

export const GroupAttributes = () => {
  const { t } = useTranslation("groups");
  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();
  const form = useForm<AttributeForm>({
    mode: "onChange",
  });

  const location = useLocation();
  const id = getLastId(location.pathname);
  const { currentGroup, subGroups, setSubGroups } = useSubGroups();

  const convertAttributes = (attr?: Record<string, any>) => {
    return arrayToKeyValue(attr || currentGroup()?.attributes!);
  };

  useEffect(() => {
    form.setValue("attributes", convertAttributes());
  }, [subGroups]);

  const save = async (attributeForm: AttributeForm) => {
    try {
      const group = currentGroup();
      const attributes = keyValueToArray(attributeForm.attributes!);
      await adminClient.groups.update({ id: id! }, { ...group, attributes });

      setSubGroups([
        ...subGroups.slice(0, subGroups.length - 1),
        { ...group, attributes },
      ]);
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
        fineGrainedAccess={currentGroup()?.access?.manage}
        reset={() =>
          form.reset({
            attributes: convertAttributes(),
          })
        }
      />
    </PageSection>
  );
};
