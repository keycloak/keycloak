import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";
import { Button, Form, Modal, ModalVariant } from "@patternfly/react-core";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { DynamicComponents } from "../../../components/dynamic/DynamicComponents";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import { ComponentTypeSelect } from "./ComponentTypeSelect";

export type IndexedComponentConfig = {
  key: string;
  value?: Record<string, unknown>;
};

export type AddComponentTypeDialogProps = {
  /** Key of the provider type in the server info `componentTypes`. */
  componentType: string;
  /** Name used for element ids and test ids, for example `validator`. */
  name: string;
  /** Translation keys for the dialog title, the empty text and the select label. */
  titleKey: string;
  emptyKey: string;
  selectLabelKey: string;
  selected: IndexedComponentConfig[];
  toggleDialog: () => void;
  onConfirm: (newComponent: ComponentRepresentation) => void;
};

export const AddComponentTypeDialog = ({
  componentType,
  name,
  titleKey,
  emptyKey,
  selectLabelKey,
  selected,
  toggleDialog,
  onConfirm,
}: AddComponentTypeDialogProps) => {
  const { t } = useTranslation();
  const [selectedComponent, setSelectedComponent] =
    useState<ComponentTypeRepresentation>();

  const allSelected =
    useServerInfo().componentTypes?.[componentType]?.length === selected.length;
  const form = useForm<ComponentTypeRepresentation>();
  const { handleSubmit } = form;

  const save = (newComponent: ComponentTypeRepresentation) => {
    onConfirm({ ...newComponent, id: selectedComponent?.id });
    toggleDialog();
  };

  return (
    <Modal
      variant={ModalVariant.small}
      title={t(titleKey)}
      isOpen
      onClose={toggleDialog}
      actions={[
        <Button
          key="save"
          data-testid={`save-${name}-role-button`}
          variant="primary"
          type="submit"
          form={`add-${name}`}
          isDisabled={!selectedComponent}
        >
          {t("save")}
        </Button>,
        <Button
          key="cancel"
          data-testid={`cancel-${name}-role-button`}
          variant="link"
          onClick={toggleDialog}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      {allSelected ? (
        t(emptyKey)
      ) : (
        <Form id={`add-${name}`} onSubmit={handleSubmit(save)}>
          <ComponentTypeSelect
            componentType={componentType}
            selected={selected.map(({ key }) => key)}
            onChange={setSelectedComponent}
            labelKey={selectLabelKey}
            fieldId={name}
          />
          {selectedComponent && (
            <FormProvider {...form}>
              <DynamicComponents properties={selectedComponent.properties} />
            </FormProvider>
          )}
        </Form>
      )}
    </Modal>
  );
};
