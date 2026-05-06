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
import type { IndexedConverters } from "../../NewAttributeSettings";
import { AddConverterDialog } from "../attribute/AddConverterDialog";

import "../../realm-settings-section.css";

export const AttributeConverters = () => {
  const { t } = useTranslation();
  const [addConverterModalOpen, toggleModal] = useToggle();
  const [converterToDelete, setConverterToDelete] = useState<string>();
  const { setValue, control, register } = useFormContext();

  const converters: IndexedConverters[] = useWatch({
    name: "converters",
    control,
    defaultValue: [],
  });

  useEffect(() => {
    register("converters");
  }, [register]);

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteConverterConfirmTitle"),
    messageKey: t("deleteConverterConfirmMsg", {
      converterName: converterToDelete,
    }),
    continueButtonLabel: "delete",
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      const updatedConverters = converters.filter(
        (converter) => converter.key !== converterToDelete,
      );

      setValue("converters", [...updatedConverters]);
    },
  });

  return (
    <>
      {addConverterModalOpen && (
        <AddConverterDialog
          selectedConverters={converters}
          onConfirm={(newConverter) => {
            setValue("converters", [
              ...converters,
              { key: newConverter.id, value: newConverter.config },
            ]);
          }}
          toggleDialog={toggleModal}
        />
      )}
      <DeleteConfirm />
      <div className="kc-attributes-validations">
        <Button
          id="addConverter"
          onClick={() => toggleModal()}
          variant="link"
          data-testid="addConverter"
          className="kc--attributes-validations--add-validation-button"
          icon={<PlusCircleIcon />}
        >
          {t("addConverter")}
        </Button>
        <Divider />
        {converters.length !== 0 ? (
          <Table>
            <Thead>
              <Tr>
                <Th>{t("converterColNames.colName")}</Th>
                <Th>{t("converterColNames.colConfig")}</Th>
                <Th aria-hidden="true" />
              </Tr>
            </Thead>
            <Tbody>
              {converters.map((converter) => (
                <Tr key={converter.key}>
                  <Td dataLabel={t("converterColNames.colName")}>
                    {converter.key}
                  </Td>
                  <Td dataLabel={t("converterColNames.colConfig")}>
                    {JSON.stringify(converter.value)}
                  </Td>
                  <Td className="kc--attributes-validations--action-cell">
                    <Button
                      key="converter"
                      variant="link"
                      data-testid="deleteConverter"
                      onClick={() => {
                        toggleDeleteDialog();
                        setConverterToDelete(converter.key);
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
          <Text className="kc-emptyConverters" component={TextVariants.p}>
            {t("emptyConverters")}
          </Text>
        )}
      </div>
    </>
  );
};
