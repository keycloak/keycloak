import {
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Tooltip,
  ValidatedOptions,
} from "@patternfly/react-core";
import { PencilAltIcon } from "@patternfly/react-icons";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import useToggle from "../../utils/useToggle";
import type { ExpandableExecution } from "../execution-model";

type EditFlowProps = {
  execution: ExpandableExecution;
  onRowChange: (execution: ExpandableExecution) => void;
};

type FormFields = Omit<ExpandableExecution, "executionList">;

export const EditFlow = ({ execution, onRowChange }: EditFlowProps) => {
  const { t } = useTranslation("authentication");
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<FormFields>({ mode: "onChange", defaultValues: execution });
  const [show, toggle] = useToggle();

  useEffect(() => reset(execution), [execution]);

  const onSubmit = (formValues: FormFields) => {
    onRowChange({ ...execution, ...formValues });
    toggle();
  };

  return (
    <>
      <Tooltip content={t("common:edit")}>
        <Button
          variant="plain"
          data-testid={`${execution.id}-edit`}
          aria-label={t("common:edit")}
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
              isDisabled={!isValid}
            >
              {t("edit")}
            </Button>,
            <Button
              data-testid="cancel"
              key="cancel"
              variant={ButtonVariant.link}
              onClick={toggle}
            >
              {t("common:cancel")}
            </Button>,
          ]}
          isOpen
        >
          <Form
            id="edit-flow-form"
            onSubmit={handleSubmit(onSubmit)}
            isHorizontal
          >
            <FormGroup
              label={t("common:name")}
              fieldId="name"
              helperTextInvalid={t("common:required")}
              validated={
                errors.displayName
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              labelIcon={
                <HelpItem
                  helpText={t("authentication-help:name")}
                  fieldLabelId="name"
                />
              }
              isRequired
            >
              <KeycloakTextInput
                id="name"
                data-testid="displayName"
                validated={
                  errors.displayName
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                {...register("displayName", { required: true })}
                isRequired
              />
            </FormGroup>
            <FormGroup
              label={t("common:description")}
              fieldId="kc-description"
              labelIcon={
                <HelpItem
                  helpText={t("authentication-help:description")}
                  fieldLabelId="description"
                />
              }
              validated={
                errors.description
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              helperTextInvalid={errors.description?.message}
            >
              <KeycloakTextArea
                id="kc-description"
                data-testid="description"
                validated={
                  errors.description
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
                {...register("description", {
                  maxLength: {
                    value: 255,
                    message: t("common:maxLength", { length: 255 }),
                  },
                })}
              />
            </FormGroup>
          </Form>
        </Modal>
      )}
    </>
  );
};
