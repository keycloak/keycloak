import { useAlerts } from "@keycloak/keycloak-ui-shared";
import { AlertVariant } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { KeyForm } from "./GenerateKeyDialog";
import type { KeyTypes } from "./SamlKeys";
import { SamlKeysDialogForm, submitForm } from "./SamlKeysDialog";

type SamlImportKeyDialogProps = {
  id: string;
  attr: KeyTypes;
  onClose: () => void;
  onImported: () => void;
};

export const SamlImportKeyDialog = ({
  id,
  attr,
  onClose,
  onImported,
}: SamlImportKeyDialogProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<SamlKeysDialogForm>();
  const {
    handleSubmit,
    formState: { isValid },
  } = form;

  const { addAlert, addError } = useAlerts();

  const submit = async (form: SamlKeysDialogForm) => {
    await submitForm(adminClient, form, id, attr, (error) => {
      if (error) {
        addError("importError", error);
      } else {
        addAlert(t("importSuccess"), AlertVariant.success);
        onImported();
      }
    });
  };

  return (
    <ConfirmDialogModal
      open={true}
      toggleDialog={onClose}
      continueButtonLabel="import"
      titleKey="importKey"
      confirmButtonDisabled={!isValid}
      onConfirm={async () => {
        await handleSubmit(submit)();
      }}
    >
      <FormProvider {...form}>
        <KeyForm useFile hasPem />
      </FormProvider>
    </ConfirmDialogModal>
  );
};
