import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { Button, Form, Modal, ModalVariant } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import type { IndexedConverters } from "../../NewAttributeSettings";
import { ConverterSelect } from "./ConverterSelect";

export type AddConverterDialogProps = {
  selectedConverters: IndexedConverters[];
  toggleDialog: () => void;
  onConfirm: (newConverter: ComponentRepresentation) => void;
};

export const AddConverterDialog = ({
  selectedConverters,
  toggleDialog,
  onConfirm,
}: AddConverterDialogProps) => {
  const { t } = useTranslation();
  const [selectedConverter, setSelectedConverter] =
    useState<ComponentTypeRepresentation>();

  const allSelected =
    useServerInfo().componentTypes?.["org.keycloak.convert.Converter"]
      ?.length === selectedConverters.length;
  const form = useForm<ComponentTypeRepresentation>();
  const { handleSubmit } = form;

  const save = (newConverter: ComponentTypeRepresentation) => {
    onConfirm({ ...newConverter, id: selectedConverter?.id });
    toggleDialog();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t("addConverter")}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          key="save"
          data-testid="save-converter-role-button"
          variant="primary"
          type="submit"
          form="add-converter"
        >
          {t("save")}
        </Button>,
        <Button
          key="cancel"
          data-testid="cancel-converter-role-button"
          variant="link"
          onClick={toggleDialog}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      {allSelected ? (
        t("emptyConverters")
      ) : (
        <Form id="add-converter" onSubmit={handleSubmit(save)}>
          <ConverterSelect
            selectedConverters={selectedConverters.map(
              (converter) => converter.key,
            )}
            onChange={setSelectedConverter}
          />
          {selectedConverter && (
            <FormProvider {...form}>
              <DynamicComponents properties={selectedConverter.properties} />
            </FormProvider>
          )}
        </Form>
      )}
    </Modal>
  );
};
