import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import type ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { Button, Form, Modal, ModalVariant } from "@patternfly/react-core";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";

export type AddValidatorRoleDialogProps = {
  open: boolean;
  toggleDialog: () => void;
  onConfirm: (newValidator: ComponentRepresentation) => void;
  selected: ComponentTypeRepresentation;
};

export const AddValidatorRoleDialog = ({
  open,
  toggleDialog,
  onConfirm,
  selected,
}: AddValidatorRoleDialogProps) => {
  const { t } = useTranslation("realm-settings");
  const form = useForm<ComponentTypeRepresentation>();
  const { handleSubmit } = form;
  const selectedRoleValidator = selected;

  const save = (newValidator: ComponentTypeRepresentation) => {
    onConfirm({ ...newValidator, id: selected.id });
    toggleDialog();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addValidatorRole", {
        validatorName: selectedRoleValidator.id,
      })}
      description={selectedRoleValidator.helpText}
      isOpen={open}
      onClose={toggleDialog}
      actions={[
        <Button
          key="save"
          data-testid="save-validator-role-button"
          variant="primary"
          onClick={() => handleSubmit(save)()}
        >
          {t("common:save")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel-validator-role-button"
          variant="link"
          onClick={toggleDialog}
        >
          {t("common:cancel")}
        </Button>,
      ]}
    >
      <Form>
        <FormProvider {...form}>
          <DynamicComponents properties={selectedRoleValidator.properties} />
        </FormProvider>
      </Form>
    </Modal>
  );
};
