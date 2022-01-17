import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
// import { Controller, useFieldArray, useFormContext } from "react-hook-form";
import {
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  TextInput,
} from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Controller, useFieldArray, useFormContext } from "react-hook-form";

import "../attribute-form/attribute-form.css";
import { defaultContextAttributes } from "../../clients/utils";
import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";

export type AttributeType = {
  key: string;
  name: string;
  custom?: boolean;
  values?: {
    [key: string]: string;
  }[];
};

type AttributeInputProps = {
  name: string;
  selectableValues?: string[];
  isKeySelectable?: boolean;
  resources?: ResourceRepresentation[];
};

export const AttributeInput = ({
  name,
  isKeySelectable,
  selectableValues,
}: AttributeInputProps) => {
  const { t } = useTranslation("common");
  const { control, register, watch } = useFormContext();
  const { fields, append, remove, insert } = useFieldArray({
    control: control,
    name,
  });

  useEffect(() => {
    if (!fields.length) {
      append({ key: "", value: "" });
    }
  }, []);

  const [isOpenArray, setIsOpenArray] = useState<boolean[]>([false]);
  const watchLastKey = watch(`${name}[${fields.length - 1}].key`, "");
  const watchLastValue = watch(`${name}[${fields.length - 1}].value`, "");

  const [valueOpen, setValueOpen] = useState(false);
  const toggleSelect = (rowIndex: number, open: boolean) => {
    const arr = [...isOpenArray];
    arr[rowIndex] = open;
    setIsOpenArray(arr);
  };

  const renderValueInput = (rowIndex: number, attribute: any) => {
    const attributeValues = defaultContextAttributes.find(
      (attr) => attr.key === attribute.key
    )?.values;

    return (
      <Td>
        {attributeValues?.length ? (
          <Controller
            name={`${name}[${rowIndex}].value`}
            defaultValue={attribute.value}
            control={control}
            render={({ onChange, value }) => (
              <Select
                id={`${attribute.id}-value`}
                className="kc-attribute-value-selectable"
                name={`${name}[${rowIndex}].value`}
                toggleId={`group-${name}`}
                onToggle={(open) => setValueOpen(open)}
                isOpen={valueOpen}
                variant={SelectVariant.typeahead}
                typeAheadAriaLabel={t("clients:selectOrTypeAKey")}
                placeholderText={t("clients:selectOrTypeAKey")}
                selections={value}
                onSelect={(_, selectedValue) => {
                  remove(rowIndex);
                  insert(rowIndex, {
                    key: attribute.key,
                    value: selectedValue,
                  });
                  onChange(selectedValue);

                  setValueOpen(false);
                }}
              >
                {attributeValues.map((attribute) => (
                  <SelectOption key={attribute.key} value={attribute.key}>
                    {t(`${attribute.name}`)}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        ) : (
          <TextInput
            id={`$clients:${attribute.key}-value`}
            className="value-input"
            name={`${name}[${rowIndex}].value`}
            ref={register()}
            defaultValue={attribute.value}
            data-testid="attribute-value-input"
          />
        )}
      </Td>
    );
  };

  return (
    <TableComposable
      className="kc-attributes__table"
      aria-label="Role attribute keys and values"
      variant="compact"
      borders={false}
    >
      <Thead>
        <Tr>
          <Th id="key" width={40}>
            {t("key")}
          </Th>
          <Th id="value" width={40}>
            {t("value")}
          </Th>
        </Tr>
      </Thead>
      <Tbody>
        {fields.map((attribute, rowIndex) => (
          <Tr key={attribute.id} data-testid="attribute-row">
            <Td>
              {isKeySelectable ? (
                <FormGroup fieldId="test">
                  <Controller
                    name={`${name}[${rowIndex}].key`}
                    defaultValue={attribute.key}
                    control={control}
                    render={({ onChange, value }) => (
                      <Select
                        toggleId="id"
                        id={`${attribute.id}-key`}
                        name={`${name}[${rowIndex}].key`}
                        className="kc-attribute-key-selectable"
                        variant={SelectVariant.typeahead}
                        typeAheadAriaLabel={t("clients:selectOrTypeAKey")}
                        placeholderText={t("clients:selectOrTypeAKey")}
                        onToggle={(open) => toggleSelect(rowIndex, open)}
                        onSelect={(_, selectedValue) => {
                          remove(rowIndex);
                          insert(rowIndex, {
                            key: selectedValue,
                            value: attribute.value,
                          });
                          onChange(selectedValue);

                          toggleSelect(rowIndex, false);
                        }}
                        selections={value}
                        aria-label="some label"
                        isOpen={isOpenArray[rowIndex]}
                      >
                        {selectableValues?.map((attribute) => (
                          <SelectOption key={attribute} value={attribute}>
                            {t(`clients:${attribute}`)}
                          </SelectOption>
                        ))}
                      </Select>
                    )}
                  />
                </FormGroup>
              ) : (
                <TextInput
                  id={`${attribute.id}-key`}
                  name={`${name}[${rowIndex}].key`}
                  ref={register()}
                  defaultValue={attribute.key}
                  data-testid="attribute-key-input"
                />
              )}
            </Td>
            {renderValueInput(rowIndex, attribute)}
            <Td key="minus-button" id={`kc-minus-button-${rowIndex}`}>
              <Button
                id={`minus-button-${rowIndex}`}
                variant="link"
                className="kc-attributes__minus-icon"
                onClick={() => remove(rowIndex)}
              >
                <MinusCircleIcon />
              </Button>
            </Td>
          </Tr>
        ))}
        <Tr>
          <Td>
            <Button
              aria-label={t("roles:addAttributeText")}
              id="plus-icon"
              variant="link"
              className="kc-attributes__plus-icon"
              onClick={() => {
                append({ key: "", value: "" });
                if (isKeySelectable) {
                  setIsOpenArray([...isOpenArray, false]);
                }
              }}
              icon={<PlusCircleIcon />}
              isDisabled={isKeySelectable ? !watchLastValue : !watchLastKey}
              data-testid="attribute-add-row"
            >
              {t("roles:addAttributeText")}
            </Button>
          </Td>
        </Tr>
      </Tbody>
    </TableComposable>
  );
};
