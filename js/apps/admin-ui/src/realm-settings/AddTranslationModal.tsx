import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import {
  FormProvider,
  SubmitHandler,
  UseFormReturn,
  useForm,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { KeyValueType } from "../components/key-value-form/key-value-convert";
import { TextControl } from "ui-shared";

type AddTranslationModalProps = {
  id?: string;
  form: UseFormReturn<TranslationForm>;
  handleModalToggle: () => void;
  save: SubmitHandler<TranslationForm>;
};

type TranslationForm = {
  key: string;
  value: string;
  translation: KeyValueType;
};

export const AddTranslationModal = ({
  handleModalToggle,
  save,
}: AddTranslationModalProps) => {
  const { t } = useTranslation();

  const form = useForm<TranslationForm>({
    mode: "onChange",
  });

  const { handleSubmit } = form;

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
      <FormProvider {...form}>
        <Form id="translation-form" onSubmit={handleSubmit(save)}>
          <TextControl
            name="key"
            label={t("key")}
            rules={{ required: t("required") }}
          />
          <TextControl
            name="value"
            label={t("value")}
            rules={{ required: t("required") }}
          />
        </Form>
      </FormProvider>
    </Modal>
  );
};
