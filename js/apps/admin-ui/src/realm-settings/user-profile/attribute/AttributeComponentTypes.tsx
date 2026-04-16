import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";
import {
  Button,
  ButtonVariant,
  Divider,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useEffect, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useConfirmDialog } from "../../../components/confirm-dialog/ConfirmDialog";
import useToggle from "../../../utils/useToggle";
import {
  AddComponentTypeDialog,
  type IndexedComponentConfig,
} from "./AddComponentTypeDialog";

import "../../realm-settings-section.css";

export type AttributeComponentTypesProps = {
  /** Name of the form field holding the list, for example `validations`. */
  fieldName: string;
  /** Key of the provider type in the server info `componentTypes`. */
  componentType: string;
  /** Capitalized name used for test ids, for example `Validator`. */
  name: string;
  i18n: {
    add: string;
    selectLabel: string;
    empty: string;
    colName: string;
    colConfig: string;
    deleteTitle: string;
    deleteMessage: string;
    /** Interpolation parameter of the delete message that holds the name. */
    deleteMessageParam: string;
  };
  /** Called before a new component is added to the form. */
  onAdd?: (newComponent: ComponentRepresentation) => void;
};

export const AttributeComponentTypes = ({
  fieldName,
  componentType,
  name,
  i18n,
  onAdd,
}: AttributeComponentTypesProps) => {
  const { t } = useTranslation();
  const [addModalOpen, toggleModal] = useToggle();
  const [toDelete, setToDelete] = useState<string>();
  const { setValue, control, register } = useFormContext();

  const components: IndexedComponentConfig[] = useWatch({
    name: fieldName,
    control,
    defaultValue: [],
  });

  useEffect(() => {
    register(fieldName);
  }, [register, fieldName]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t(i18n.deleteTitle),
    messageKey: t(i18n.deleteMessage, {
      [i18n.deleteMessageParam]: toDelete,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      setValue(
        fieldName,
        components.filter((component) => component.key !== toDelete),
      );
    },
  });

  return (
    <>
      {addModalOpen && (
        <AddComponentTypeDialog
          componentType={componentType}
          name={name.toLowerCase()}
          titleKey={i18n.add}
          emptyKey={i18n.empty}
          selectLabelKey={i18n.selectLabel}
          selected={components}
          onConfirm={(newComponent) => {
            onAdd?.(newComponent);
            setValue(fieldName, [
              ...components,
              { key: newComponent.id, value: newComponent.config },
            ]);
          }}
          toggleDialog={toggleModal}
        />
      )}
      <DeleteConfirm />
      <div className="kc-attributes-validations">
        <Button
          id={`add${name}`}
          onClick={() => toggleModal()}
          variant="link"
          data-testid={`add${name}`}
          className="kc--attributes-validations--add-validation-button"
          icon={<PlusCircleIcon />}
        >
          {t(i18n.add)}
        </Button>
        <Divider />
        {components.length !== 0 ? (
          <Table>
            <Thead>
              <Tr>
                <Th>{t(i18n.colName)}</Th>
                <Th>{t(i18n.colConfig)}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {components.map((component) => (
                <Tr key={component.key}>
                  <Td dataLabel={t(i18n.colName)}>{component.key}</Td>
                  <Td dataLabel={t(i18n.colConfig)}>
                    {JSON.stringify(component.value)}
                  </Td>
                  <Td className="kc--attributes-validations--action-cell">
                    <Button
                      key={name.toLowerCase()}
                      variant="link"
                      data-testid={`delete${name}`}
                      onClick={() => {
                        toggleDeleteDialog();
                        setToDelete(component.key);
                      }}
                    >
                      {t("delete")}
                    </Button>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        ) : (
          <Text className={`kc-empty${name}s`} component={TextVariants.p}>
            {t(i18n.empty)}
          </Text>
        )}
      </div>
    </>
  );
};
