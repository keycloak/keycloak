import { TextControl } from "@keycloak/keycloak-ui-shared";
import { Form } from "@patternfly/react-core";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm } from "react-hook-form";

type FileNameDialogProps = {
  onSave: (fileName: string) => void;
  onClose: () => void;
};

type FormValues = {
  fileName: string;
};
export const FileNameDialog = ({ onSave, onClose }: FileNameDialogProps) => {
  const { t } = useTranslation();
  const form = useForm<FormValues>();
  const { handleSubmit } = form;

  const save = ({ fileName }: FormValues) => onSave(fileName);
  return (
    <ConfirmDialogModal
      titleKey="fileNameDialogTitle"
      open
      toggleDialog={onClose}
      onConfirm={() => handleSubmit(save)()}
    >
      <Form isHorizontal onSubmit={handleSubmit(save)}>
        <FormProvider {...form}>
          <TextControl
            name="fileName"
            label={t("fileName")}
            defaultValue="quick-theme.jar"
          />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
