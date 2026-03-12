import { TextControl } from "@keycloak/keycloak-ui-shared";
import { Form } from "@patternfly/react-core";
import { ModalVariant } from "@patternfly/react-core/deprecated";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { useTranslation } from "react-i18next";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { useEffect } from "react";

type FileNameDialogProps = {
  onSave: (
    themeName: string,
    fileName: string,
    themeDescription: string,
  ) => void;
  onClose: () => void;
};

type FormValues = {
  themeName: string;
  fileName: string;
  themeDescription: string;
};

export const FileNameDialog = ({ onSave, onClose }: FileNameDialogProps) => {
  const { t } = useTranslation();
  const form = useForm<FormValues>({
    defaultValues: {
      themeName: "quick-theme",
      fileName: "quick-theme.jar",
      themeDescription: t("themeDescriptionDefault"),
    },
  });
  const { handleSubmit, setValue, control } = form;

  const themeName = useWatch({ control, name: "themeName" });

  // Auto-update fileName when themeName changes
  useEffect(() => {
    setValue("fileName", `${themeName?.trim()}.jar`);
  }, [themeName, setValue]);

  const save = ({ themeName, fileName, themeDescription }: FormValues) =>
    onSave(themeName, fileName, themeDescription);

  return (
    <ConfirmDialogModal
      titleKey="fileNameDialogTitle"
      open
      variant={ModalVariant.medium}
      toggleDialog={onClose}
      onConfirm={() => handleSubmit(save)()}
    >
      <Form isHorizontal onSubmit={handleSubmit(save)}>
        <FormProvider {...form}>
          <TextControl name="themeName" label={t("themeName")} />
          <TextControl name="fileName" label={t("fileName")} />
          <TextControl name="themeDescription" label={t("themeDescription")} />
        </FormProvider>
      </Form>
    </ConfirmDialogModal>
  );
};
