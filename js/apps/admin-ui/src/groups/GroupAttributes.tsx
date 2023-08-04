import {
  AlertVariant,
  PageSection,
  PageSectionVariants,
} from "@patternfly/react-core";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";

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
import { useSubGroups } from "./SubGroupsContext";
import { getLastId } from "./groupIdUtils";

export const GroupAttributes = () => {
  const { t } = useTranslation("groups");
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
