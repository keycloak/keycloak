import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Modal,
  ModalVariant,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

import type { IndexedValidations } from "../../NewAttributeSettings";
import { AddValidatorRoleDialog } from "./AddValidatorRoleDialog";
import useToggle from "../../../utils/useToggle";
import { useServerInfo } from "../../../context/server-info/ServerInfoProvider";
import ComponentTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentTypeRepresentation";

export type AddValidatorDialogProps = {
  selectedValidators: IndexedValidations[];
  toggleDialog: () => void;
  onConfirm: (newValidator: ComponentTypeRepresentation) => void;
};

export const AddValidatorDialog = ({
  selectedValidators,
  toggleDialog,
  onConfirm,
}: AddValidatorDialogProps) => {
  const { t } = useTranslation("realm-settings");
  const [selectedValidator, setSelectedValidator] =
    useState<ComponentTypeRepresentation>();
  const allValidator: ComponentTypeRepresentation[] =
    useServerInfo().componentTypes?.["org.keycloak.validate.Validator"] || [];
  const [validators, setValidators] = useState(
    allValidator.filter(
      ({ id }) => !selectedValidators.map(({ key }) => key).includes(id)
    )
  );
  const [addValidatorRoleModalOpen, toggleModal] = useToggle();

  return (
    <>
      {addValidatorRoleModalOpen && (
        <AddValidatorRoleDialog
          onConfirm={(newValidator) => {
            onConfirm(newValidator);
            setValidators(
              validators.filter(({ id }) => id !== newValidator.id)
            );
          }}
          open={addValidatorRoleModalOpen}
          toggleDialog={toggleModal}
          selected={selectedValidator!}
        />
      )}
      <Modal
        variant={ModalVariant.small}
        title={t("addValidator")}
        isOpen
        onClose={toggleDialog}
      >
        {validators.length !== 0 ? (
          <TableComposable>
            <Thead>
              <Tr>
                <Th>{t("validatorDialogColNames.colName")}</Th>
                <Th>{t("validatorDialogColNames.colDescription")}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {allValidator.map((validator) => (
                <Tr
                  key={validator.id}
                  onRowClick={() => {
                    setSelectedValidator(validator);
                    toggleModal();
                  }}
                  isHoverable
                >
                  <Td dataLabel={t("validatorDialogColNames.colName")}>
                    {validator.id}
                  </Td>
                  <Td dataLabel={t("validatorDialogColNames.colDescription")}>
                    {validator.helpText}
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </TableComposable>
        ) : (
          <Text className="kc-emptyValidators" component={TextVariants.h6}>
            {t("realm-settings:emptyValidators")}
          </Text>
        )}
      </Modal>
    </>
  );
};
