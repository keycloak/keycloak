import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";
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

import type { ExpandableExecution } from "../execution-model";
import useToggle from "../../utils/useToggle";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { KeycloakTextArea } from "../../components/keycloak-text-area/KeycloakTextArea";

type EditFlowProps = {
  execution: ExpandableExecution;
  onRowChange: (execution: ExpandableExecution) => void;
};

export const EditFlow = ({ execution, onRowChange }: EditFlowProps) => {
  const { t } = useTranslation("authentication");
  const { register, errors, reset, handleSubmit } = useForm();

  const [show, toggle] = useToggle();

  const update = (values: ExpandableExecution) => {
    onRowChange({ ...execution, ...values });
    toggle();
  };

  useEffect(() => {
    reset(execution);
  }, [execution, reset]);

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
          isOpen={true}
          onClose={toggle}
          variant={ModalVariant.small}
          actions={[
            <Button
              id="modal-confirm"
              key="confirm"
              onClick={handleSubmit(update)}
              data-testid="confirm"
            >
              {t("edit")}
            </Button>,
            <Button
              data-testid="cancel"
              id="modal-cancel"
              key="cancel"
              variant={ButtonVariant.link}
              onClick={toggle}
            >
              {t("common:cancel")}
            </Button>,
          ]}
        >
          <Form isHorizontal>
            <FormGroup
              label={t("common:name")}
              fieldId="name"
              helperTextInvalid={t("common:required")}
              validated={
                errors.displayName
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isRequired
              labelIcon={
                <HelpItem
                  helpText="authentication-help:name"
                  fieldLabelId="name"
                />
              }
            >
              <KeycloakTextInput
                type="text"
                id="name"
                name="displayName"
                data-testid="displayName"
                ref={register({ required: true })}
                validated={
                  errors.displayName
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
            <FormGroup
              label={t("common:description")}
              fieldId="kc-description"
              labelIcon={
                <HelpItem
                  helpText="authentication-help:description"
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
                ref={register({
                  maxLength: {
                    value: 255,
                    message: t("common:maxLength", { length: 255 }),
                  },
                })}
                type="text"
                id="kc-description"
                name="description"
                data-testid="description"
                validated={
                  errors.description
                    ? ValidatedOptions.error
                    : ValidatedOptions.default
                }
              />
            </FormGroup>
          </Form>
        </Modal>
      )}
    </>
  );
};
