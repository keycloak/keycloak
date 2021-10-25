import React, { useEffect } from "react";
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

import { FormAccess } from "../form-access/FormAccess";

import "./attribute-form.css";

export type KeyValueType = { key: string; value: string };

export type AttributeForm = {
  attributes: KeyValueType[];
};

export type AttributesFormProps = {
  form: UseFormMethods<AttributeForm>;
  save?: (model: AttributeForm) => void;
  reset?: () => void;
  array: {
    fields: Partial<ArrayField<Record<string, any>, "id">>[];
    append: (
      value: Partial<Record<string, any>> | Partial<Record<string, any>>[],
      shouldFocus?: boolean | undefined
    ) => void;
    remove: (index?: number | number[] | undefined) => void;
  };
  inConfig?: boolean;
};

export const arrayToAttributes = (attributeArray: KeyValueType[]) => {
  const initValue: { [index: string]: string[] } = {};
  return attributeArray.reduce((acc, attribute) => {
    acc[attribute.key] = [attribute.value];
    return acc;
  }, initValue);
};

export const attributesToArray = (attributes?: {
  [key: string]: string[];
}): KeyValueType[] => {
  if (!attributes || Object.keys(attributes).length == 0) {
    return [];
  }
  return Object.keys(attributes).map((key) => ({
    key: key,
    value: attributes[key][0],
  }));
};

export const AttributesForm = ({
  form: { handleSubmit, register, formState, errors, watch },
  array: { fields, append, remove },
  reset,
  save,
  inConfig,
}: AttributesFormProps) => {
  const { t } = useTranslation("roles");

  const columns = ["Key", "Value"];

  const noSaveCancelButtons = !save && !reset;

  const watchLast = inConfig
    ? watch(`config.attributes[${fields.length - 1}].key`, "")
    : watch(`attributes[${fields.length - 1}].key`, "");

  useEffect(() => {
    if (fields.length === 0) {
      append({ key: "", value: "" });
    }
  }, [fields]);

  return (
    <FormAccess
      role="manage-realm"
      onSubmit={save ? handleSubmit(save) : undefined}
    >
      <TableComposable
        className="kc-attributes__table"
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
                  name={
                    inConfig
                      ? `config.attributes[${rowIndex}].key`
                      : `attributes[${rowIndex}].key`
                  }
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
                  name={
                    inConfig
                      ? `config.attributes[${rowIndex}].value`
                      : `attributes[${rowIndex}].value`
                  }
                  ref={register()}
                  aria-label="value-input"
                  defaultValue={attribute.value}
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
                    className="kc-attributes__minus-icon"
                    onClick={() => remove(rowIndex)}
                  >
                    <MinusCircleIcon />
                  </Button>
                </Td>
              )}
              {rowIndex === fields.length - 1 && (
                <Td key="add-button" id="add-button" dataLabel={columns[2]}>
                  {fields.length !== 1 && (
                    <Button
                      id={`minus-button-${rowIndex}`}
                      aria-label={`remove ${attribute.key} with value ${attribute.value} `}
                      variant="link"
                      className="kc-attributes__minus-icon"
                      onClick={() => remove(rowIndex)}
                    >
                      <MinusCircleIcon />
                    </Button>
                  )}
                </Td>
              )}
            </Tr>
          ))}
          <Tr>
            <Td>
              <Button
                aria-label={t("roles:addAttributeText")}
                id="plus-icon"
                variant="link"
                className="kc-attributes__plus-icon"
                onClick={() => append({ key: "", value: "" })}
                icon={<PlusCircleIcon />}
                isDisabled={!watchLast}
              >
                {t("roles:addAttributeText")}
              </Button>
            </Td>
          </Tr>
        </Tbody>
      </TableComposable>
      {!noSaveCancelButtons && (
        <ActionGroup className="kc-attributes__action-group">
          <Button
            data-testid="save-attributes"
            variant="primary"
            type="submit"
            isDisabled={!watchLast}
          >
            {t("common:save")}
          </Button>
          <Button
            onClick={reset}
            variant="link"
            isDisabled={!formState.isDirty}
          >
            {t("common:revert")}
          </Button>
        </ActionGroup>
      )}
    </FormAccess>
  );
};
