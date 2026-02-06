import { TextControl } from "@keycloak/keycloak-ui-shared";
import { Form } from "@patternfly/react-core";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useEffect } from "react";

type FileNameDialogProps = {
  onSave: (themeName: string, fileName: string) => void;
  onClose: () => void;
};

type FormValues = {
  themeName: string;
  fileName: string;
};

export const FileNameDialog = ({ onSave, onClose }: FileNameDialogProps) => {
  const { t } = useTranslation();
  const form = useForm<FormValues>({
    defaultValues: {
      themeName: "quick-theme",
      fileName: "quick-theme.jar",
    },
  });
  const { handleSubmit, setValue, control } = form;

  const themeName = useWatch({ control, name: "themeName" });

  // Auto-update fileName when themeName changes
  useEffect(() => {
    setValue("themeName", themeName);
    setValue("fileName", `${themeName?.trim()}.jar`);
  }, [themeName, setValue]);

  const save = ({ themeName, fileName }: FormValues) =>
    onSave(themeName, fileName);

  return (
    <ConfirmDialogModal
      titleKey="fileNameDialogTitle"
      open
      toggleDialog={onClose}
      onConfirm={() => handleSubmit(save)()}
    >
      <Form isHorizontal onSubmit={handleSubmit(save)}>
        <FormProvider {...form}>
          <TextControl name="themeName" label={t("themeName")} />
          <TextControl name="fileName" label={t("fileName")} />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
