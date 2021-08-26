import React from "react";
import { useTranslation } from "react-i18next";
import type { ArrayField, UseFormMethods } from "react-hook-form";
import { ActionGroup, Button, TextInput } from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";

import { FormAccess } from "../components/form-access/FormAccess";
import type { RoleFormType } from "./RealmRoleTabs";

import "./RealmRolesSection.css";

export type KeyValueType = { key: string; value: string };

type RoleAttributesProps = {
  form: UseFormMethods<RoleFormType>;
  save: () => void;
  reset: () => void;
  array: {
    fields: Partial<ArrayField<Record<string, any>, "id">>[];
    append: (
      value: Partial<Record<string, any>> | Partial<Record<string, any>>[],
      shouldFocus?: boolean | undefined
    ) => void;
    remove: (index?: number | number[] | undefined) => void;
  };
};

export const RoleAttributes = ({
  form: { register, formState, errors, watch },
  save,
  array: { fields, append, remove },
  reset,
}: RoleAttributesProps) => {
  const { t } = useTranslation("roles");

  const columns = ["Key", "Value"];
  const watchFirstKey = watch("attributes[0].key", "");

  return (
    <FormAccess role="manage-realm">
      <TableComposable
        className="kc-role-attributes__table"
        aria-label="Role attribute keys and values"
        variant="compact"
        borders={false}
      >
        <Thead>
          <Tr>
            <Th id="key" width={40}>
              {columns[0]}
            </Th>
            <Th id="value" width={40}>
              {columns[1]}
            </Th>
          </Tr>
        </Thead>
        <Tbody>
          {fields.map((attribute, rowIndex) => (
            <Tr key={attribute.id}>
              <Td
                key={`${attribute.id}-key`}
                id={`text-input-${rowIndex}-key`}
                dataLabel={columns[0]}
              >
                <TextInput
                  name={`attributes[${rowIndex}].key`}
                  ref={register()}
                  aria-label="key-input"
                  defaultValue={attribute.key}
                  validated={
                    errors.attributes?.[rowIndex] ? "error" : "default"
                  }
                />
              </Td>
              <Td
                key={`${attribute}-value`}
                id={`text-input-${rowIndex}-value`}
                dataLabel={columns[1]}
              >
                <TextInput
                  name={`attributes[${rowIndex}].value`}
                  ref={register()}
                  aria-label="value-input"
                  defaultValue={attribute.value}
                  validated={errors.description ? "error" : "default"}
                />
              </Td>
              {rowIndex !== fields.length - 1 && fields.length - 1 !== 0 && (
                <Td
                  key="minus-button"
                  id={`kc-minus-button-${rowIndex}`}
                  dataLabel={columns[2]}
                >
                  <Button
                    id={`minus-button-${rowIndex}`}
                    aria-label={`remove ${attribute.key} with value ${attribute.value} `}
                    variant="link"
                    className="kc-role-attributes__minus-icon"
                    onClick={() => remove(rowIndex)}
                  >
                    <MinusCircleIcon />
                  </Button>
                </Td>
              )}
              {rowIndex === fields.length - 1 && (
                <Td key="add-button" id="add-button" dataLabel={columns[2]}>
                  {fields[rowIndex].key === "" && (
                    <Button
                      id={`minus-button-${rowIndex}`}
                      aria-label={`remove ${attribute.key} with value ${attribute.value} `}
                      variant="link"
                      className="kc-role-attributes__minus-icon"
                      onClick={() => remove(rowIndex)}
                    >
                      <MinusCircleIcon />
                    </Button>
                  )}
                  <Button
                    aria-label={t("roles:addAttributeText")}
                    id="plus-icon"
                    variant="link"
                    className="kc-role-attributes__plus-icon"
                    onClick={() => append({ key: "", value: "" })}
                    icon={<PlusCircleIcon />}
                    isDisabled={!formState.isValid}
                  />
                </Td>
              )}
            </Tr>
          ))}
        </Tbody>
      </TableComposable>
      <ActionGroup className="kc-role-attributes__action-group">
        <Button
          data-testid="realm-roles-save-button"
          variant="primary"
          isDisabled={!watchFirstKey}
          onClick={save}
        >
          {t("common:save")}
        </Button>
        <Button onClick={reset} variant="link">
          {t("common:reload")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
