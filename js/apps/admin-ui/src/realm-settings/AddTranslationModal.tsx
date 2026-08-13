import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { FormProvider, SubmitHandler, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { TextControl } from "@keycloak/keycloak-ui-shared";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";

type AddTranslationModalProps = {
  id?: string;
  form: UseFormReturn<TranslationForm>;
  save: SubmitHandler<TranslationForm>;
  handleModalToggle: () => void;
};

export type TranslationForm = {
  key: string;
  value: string;
  translation: KeyValueType;
};

export const AddTranslationModal = ({
  handleModalToggle,
  save,
  form,
}: AddTranslationModalProps) => {
  const { t } = useTranslation();

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addTranslation")}
      isOpen
      onClose={handleModalToggle}
      actions={[
        <Button
          data-testid="add-translation-confirm-button"
          key="confirm"
          variant="primary"
          type="submit"
          form="translation-form"
        >
          {t("create")}
        </Button>,
        <Button
          id="modal-cancel"
          data-testid="cancel"
          key="cancel"
          variant={ButtonVariant.link}
          onClick={() => {
            handleModalToggle();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form
        id="translation-form"
        isHorizontal
        onSubmit={form.handleSubmit(save)}
      >
        <FormProvider {...form}>
          <TextControl
            name="key"
            label={t("key")}
            autoFocus
            rules={{
              required: t("required"),
            }}
          />
          <TextControl
            name="value"
            label={t("value")}
            rules={{
              required: t("required"),
            }}
          />
        </FormProvider>
      </Form>
    </Modal>
  );
};
