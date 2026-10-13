import {
  Button,
  ButtonVariant,
  Form,
  Modal,
  ModalVariant,
  Tooltip,
} from "@patternfly/react-core";
import { PencilAltIcon } from "@patternfly/react-icons";
import { useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { TextAreaControl, TextControl } from "@keycloak/keycloak-ui-shared";
import useToggle from "../../utils/useToggle";
import type { ExpandableExecution } from "../execution-model";

type EditFlowProps = {
  execution: ExpandableExecution;
  onRowChange: (execution: ExpandableExecution) => void;
};

type FormFields = Omit<ExpandableExecution, "executionList">;

export const EditFlow = ({ execution, onRowChange }: EditFlowProps) => {
  const { t } = useTranslation();
  const form = useForm<FormFields>({
    mode: "onChange",
    defaultValues: execution,
  });
  const [show, toggle] = useToggle();

  useEffect(() => form.reset(execution), [execution]);

  const onSubmit = (formValues: FormFields) => {
    onRowChange({ ...execution, ...formValues });
    toggle();
  };

  return (
    <>
      <Tooltip content={t("edit")}>
        <Button
          variant="plain"
          data-testid={`${execution.id}-edit`}
          aria-label={t("edit")}
          onClick={toggle}
        >
          <PencilAltIcon />
        </Button>
      </Tooltip>
      {show && (
        <Modal
          title={t("editFlow")}
          onClose={toggle}
          variant={ModalVariant.small}
          actions={[
            <Button
              key="confirm"
              data-testid="confirm"
              type="submit"
              form="edit-flow-form"
              isDisabled={!form.formState.isValid}
            >
              {t("edit")}
            </Button>,
            <Button
              data-testid="cancel"
              key="cancel"
              variant={ButtonVariant.link}
              onClick={toggle}
            >
              {t("cancel")}
            </Button>,
          ]}
          isOpen
        >
          <Form
            id="edit-flow-form"
            onSubmit={form.handleSubmit(onSubmit)}
            isHorizontal
          >
            <FormProvider {...form}>
              <TextControl
                name="displayName"
                label={t("name")}
                labelIcon={t("flowNameHelp")}
                rules={{ required: t("required") }}
              />
              <TextAreaControl
                name="description"
                label={t("description")}
                labelIcon={t("flowDescriptionHelp")}
                rules={{
                  maxLength: {
                    value: 255,
                    message: t("maxLength", { length: 255 }),
                  },
                }}
              />
            </FormProvider>
          </Form>
        </Modal>
      )}
    </>
  );
};
