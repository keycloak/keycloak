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
import { DefaultValue } from "../../../components/key-value-form/KeyValueInput";
import useToggle from "../../../utils/useToggle";
import type { IndexedValidations } from "../../NewAttributeSettings";
import { AddValidatorDialog } from "../attribute/AddValidatorDialog";

import "../../realm-settings-section.css";

export const AttributeValidations = () => {
  const { t } = useTranslation();
  const [addValidatorModalOpen, toggleModal] = useToggle();
  const [validatorToDelete, setValidatorToDelete] = useState<string>();
  const { setValue, control, register, getValues } = useFormContext();

  const validators: IndexedValidations[] = useWatch({
    name: "validations",
    control,
    defaultValue: [],
  });

  useEffect(() => {
    register("validations");
  }, [register]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteValidatorConfirmTitle"),
    messageKey: t("deleteValidatorConfirmMsg", {
      validatorName: validatorToDelete,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      const updatedValidators = validators.filter(
        (validator) => validator.key !== validatorToDelete,
      );

      setValue("validations", [...updatedValidators]);
    },
  });

  return (
    <>
      {addValidatorModalOpen && (
        <AddValidatorDialog
          selectedValidators={validators}
          onConfirm={(newValidator) => {
            const annotations: DefaultValue[] = getValues("annotations");
            if (
              newValidator.id === "options" &&
              !annotations.find((a) => a.key === "inputType")
            ) {
              setValue("annotations", [
                ...annotations,
                { key: "inputType", value: "select" },
              ]);
            }
            setValue("validations", [
              ...validators,
              { key: newValidator.id, value: newValidator.config },
            ]);
          }}
          toggleDialog={toggleModal}
        />
      )}
      <DeleteConfirm />
      <div className="kc-attributes-validations">
        <Button
          id="addValidator"
          onClick={() => toggleModal()}
          variant="link"
          data-testid="addValidator"
          className="kc--attributes-validations--add-validation-button"
          icon={<PlusCircleIcon />}
        >
          {t("addValidator")}
        </Button>
        <Divider />
        {validators.length !== 0 ? (
          <Table>
            <Thead>
              <Tr>
                <Th>{t("validatorColNames.colName")}</Th>
                <Th>{t("validatorColNames.colConfig")}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {validators.map((validator) => (
                <Tr key={validator.key}>
                  <Td dataLabel={t("validatorColNames.colName")}>
                    {validator.key}
                  </Td>
                  <Td dataLabel={t("validatorColNames.colConfig")}>
                    {JSON.stringify(validator.value)}
                  </Td>
                  <Td className="kc--attributes-validations--action-cell">
                    <Button
                      key="validator"
                      variant="link"
                      data-testid="deleteValidator"
                      onClick={() => {
                        toggleDeleteDialog();
                        setValidatorToDelete(validator.key);
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
          <Text className="kc-emptyValidators" component={TextVariants.p}>
            {t("emptyValidators")}
          </Text>
        )}
      </div>
    </>
  );
};
