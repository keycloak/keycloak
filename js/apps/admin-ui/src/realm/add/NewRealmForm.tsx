import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  FormSubmitButton,
  TextControl,
  useAlerts,
} from "@keycloak/keycloak-ui-shared";
import { Button, Modal } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { JsonFileUpload } from "../../components/json-file-upload/JsonFileUpload";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { convertFormValuesToObject, convertToFormValues } from "../../util";
import { toRealm } from "../RealmRoutes";

type NewRealmFormProps = {
  onClose: () => void;
};

export default function NewRealmForm({ onClose }: NewRealmFormProps) {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const navigate = useNavigate();
  const { refresh, whoAmI } = useWhoAmI();
  const { addAlert, addError } = useAlerts();
  const [realm, setRealm] = useState<RealmRepresentation>();

  const form = useForm<RealmRepresentation>({
    mode: "onChange",
  });

  const { handleSubmit, setValue, formState } = form;

  const handleFileChange = (obj?: object) => {
    const defaultRealm = { id: "", realm: "", enabled: true };
    convertToFormValues(obj || defaultRealm, setValue);
    setRealm(obj || defaultRealm);
  };

  const save = async (fields: RealmRepresentation) => {
    try {
      await adminClient.realms.create({
        ...realm,
        ...convertFormValuesToObject(fields),
      });
      addAlert(t("saveRealmSuccess"));

      refresh();
      onClose();
      navigate(toRealm({ realm: fields.realm! }));
    } catch (error) {
      addError("saveRealmError", error);
    }
  };

  return (
    <Modal
      variant="medium"
      title={t("createRealm")}
      description={t("realmExplain")}
      onClose={onClose}
      isOpen
      actions={[
        <FormSubmitButton
          form="realm-form"
          data-testid="create"
          formState={formState}
          allowInvalid
          allowNonDirty
          key="confirm"
        >
          {t("create")}
        </FormSubmitButton>,
        <Button
          variant="link"
          onClick={onClose}
          key={"cancel"}
          data-testid="cancel"
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <FormProvider {...form}>
        <FormAccess
          id="realm-form"
          isHorizontal
          onSubmit={handleSubmit(save)}
          role="view-realm"
          isReadOnly={!whoAmI.createRealm}
        >
          <JsonFileUpload
            id="kc-realm-filename"
            allowEditingUploadedText
            onChange={handleFileChange}
          />
          <TextControl
            name="realm"
            label={t("realmNameField")}
            rules={{ required: t("required") }}
          />
          <DefaultSwitchControl
            name="enabled"
            label={t("enabled")}
            defaultValue={true}
          />
        </FormAccess>
      </FormProvider>
    </Modal>
  );
}
