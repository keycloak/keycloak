import { useTranslation } from "react-i18next";
import { FormProvider, useFormContext } from "react-hook-form-v7";
import { AlertVariant } from "@patternfly/react-core";

import type { KeyTypes } from "./SamlKeys";
import { KeyForm } from "./GenerateKeyDialog";
import { useAdminClient } from "../../context/auth/AdminClient";
import { useAlerts } from "../../components/alert/Alerts";
import { SamlKeysDialogForm, submitForm } from "./SamlKeysDialog";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";

type SamlImportKeyDialogProps = {
  id: string;
  attr: KeyTypes;
  onClose: () => void;
};

export const SamlImportKeyDialog = ({
  id,
  attr,
  onClose,
}: SamlImportKeyDialogProps) => {
  const { t } = useTranslation("clients");
  const form = useFormContext<SamlKeysDialogForm>();
  const { handleSubmit } = form;

  const { adminClient } = useAdminClient();
  const { addAlert, addError } = useAlerts();

  const submit = (form: SamlKeysDialogForm) => {
    submitForm(form, id, attr, adminClient, (error) => {
      if (error) {
        addError("clients:importError", error);
      } else {
        addAlert(t("importSuccess"), AlertVariant.success);
      }
    });
  };

  return (
    <ConfirmDialogModal
      open={true}
      toggleDialog={onClose}
      continueButtonLabel="clients:import"
      titleKey="clients:importKey"
      onConfirm={() => {
        handleSubmit(submit)();
        onClose();
      }}
    >
      <FormProvider {...form}>
        <KeyForm useFile hasPem />
      </FormProvider>
    </ConfirmDialogModal>
  );
};
