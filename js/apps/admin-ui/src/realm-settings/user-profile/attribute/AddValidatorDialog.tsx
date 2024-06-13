import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { Button, Form, Modal, ModalVariant } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import type { IndexedValidations } from "../../NewAttributeSettings";
import { ValidatorSelect } from "./ValidatorSelect";

export type AddValidatorDialogProps = {
  selectedValidators: IndexedValidations[];
  toggleDialog: () => void;
  onConfirm: (newValidator: ComponentRepresentation) => void;
};

export const AddValidatorDialog = ({
  selectedValidators,
  toggleDialog,
  onConfirm,
}: AddValidatorDialogProps) => {
  const { t } = useTranslation();
  const [selectedValidator, setSelectedValidator] =
    useState<ComponentTypeRepresentation>();

  const allSelected =
    useServerInfo().componentTypes?.["org.keycloak.validate.Validator"]
      .length === selectedValidators.length;
  const form = useForm<ComponentTypeRepresentation>();
  const { handleSubmit } = form;

  const save = (newValidator: ComponentTypeRepresentation) => {
    onConfirm({ ...newValidator, id: selectedValidator?.id });
    toggleDialog();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addValidator")}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          key="save"
          data-testid="save-validator-role-button"
          variant="primary"
          type="submit"
          form="add-validator"
        >
          {t("save")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel-validator-role-button"
          variant="link"
          onClick={toggleDialog}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      {allSelected ? (
        t("emptyValidators")
      ) : (
        <Form id="add-validator" onSubmit={handleSubmit(save)}>
          <ValidatorSelect
            selectedValidators={selectedValidators.map(
              (validator) => validator.key,
            )}
            onChange={setSelectedValidator}
          />
          {selectedValidator && (
            <FormProvider {...form}>
              <DynamicComponents properties={selectedValidator.properties} />
            </FormProvider>
          )}
        </Form>
      )}
    </Modal>
  );
};
