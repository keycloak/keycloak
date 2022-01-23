import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFieldArray, useFormContext } from "react-hook-form";
import {
  Button,
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

import "../attribute-form/attribute-form.css";
import { defaultContextAttributes } from "../../clients/utils";
import { camelCase } from "lodash";
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
  resources,
}: AttributeInputProps) => {
  const { t } = useTranslation("common");
  const { control, register, watch, getValues } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control: control,
    name,
  });

  useEffect(() => {
    if (!fields.length) {
      append({ key: "", value: "" });
    }
  }, []);

  const [isKeyOpenArray, setIsKeyOpenArray] = useState([false]);
  const watchLastKey = watch(`${name}[${fields.length - 1}].key`, "");
  const watchLastValue = watch(`${name}[${fields.length - 1}].value`, "");

  const [isValueOpenArray, setIsValueOpenArray] = useState([false]);
  const toggleKeySelect = (rowIndex: number, open: boolean) => {
    const arr = [...isKeyOpenArray];
    arr[rowIndex] = open;
    setIsKeyOpenArray(arr);
  };

  const toggleValueSelect = (rowIndex: number, open: boolean) => {
    const arr = [...isValueOpenArray];
    arr[rowIndex] = open;
    setIsValueOpenArray(arr);
  };

  const renderValueInput = (rowIndex: number, attribute: any) => {
    let attributeValues: { key: string; name: string }[] | undefined = [];

    const scopeValues = resources?.find(
      (resource) => resource.name === getValues().resources[rowIndex]?.key
    )?.scopes;

    if (selectableValues) {
      attributeValues = defaultContextAttributes.find(
        (attr) => attr.name === getValues().context[rowIndex]?.key
      )?.values;
    }

    const getMessageBundleKey = (attributeName: string) =>
      camelCase(attributeName).replace(/\W/g, "");

    return (
      <Td>
        {scopeValues?.length || attributeValues?.length ? (
          <Controller
            name={`${name}[${rowIndex}].value`}
            defaultValue={[]}
            control={control}
            render={({ onChange, value }) => (
              <Select
                id={`${attribute.id}-value`}
                className="kc-attribute-value-selectable"
                name={`${name}[${rowIndex}].value`}
                chipGroupProps={{
                  numChips: 1,
                  expandedText: t("common:hide"),
                  collapsedText: t("common:showRemaining"),
                }}
                toggleId={`group-${name}`}
                onToggle={(open) => toggleValueSelect(rowIndex, open)}
                isOpen={isValueOpenArray[rowIndex]}
                variant={
                  resources
                    ? SelectVariant.typeaheadMulti
                    : SelectVariant.typeahead
                }
                typeAheadAriaLabel={t("clients:selectOrTypeAKey")}
                placeholderText={t("clients:selectOrTypeAKey")}
                selections={value}
                onSelect={(_, v) => {
                  if (resources) {
                    const option = v.toString();
                    if (value.includes(option)) {
                      onChange(value.filter((item: string) => item !== option));
                    } else {
                      onChange([...value, option]);
                    }
                  } else {
                    onChange(v);
                  }
                  toggleValueSelect(rowIndex, false);
                }}
              >
                {(scopeValues || attributeValues)?.map((scope) => (
                  <SelectOption key={scope.name} value={scope.name} />
                ))}
              </Select>
            )}
          />
        ) : (
          <TextInput
            id={`${getMessageBundleKey(attribute.key)}-value`}
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
                <Controller
                  name={`${name}[${rowIndex}].key`}
                  defaultValue={attribute.key}
                  control={control}
                  render={({ onChange, value }) => (
                    <Select
                      id={`${name}[${rowIndex}].key`}
                      className="kc-attribute-key-selectable"
                      name={`${name}[${rowIndex}].key`}
                      toggleId={`group-${name}`}
                      onToggle={(open) => toggleKeySelect(rowIndex, open)}
                      isOpen={isKeyOpenArray[rowIndex]}
                      variant={SelectVariant.typeahead}
                      typeAheadAriaLabel={t("clients:selectOrTypeAKey")}
                      placeholderText={t("clients:selectOrTypeAKey")}
                      selections={value}
                      onSelect={(_, v) => {
                        onChange(v);

                        toggleKeySelect(rowIndex, false);
                      }}
                    >
                      {selectableValues?.map((attribute) => (
                        <SelectOption
                          selected={attribute === value}
                          key={attribute}
                          value={attribute}
                        >
                          {attribute}
                        </SelectOption>
                      ))}
                    </Select>
                  )}
                />
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
                  setIsKeyOpenArray([...isKeyOpenArray, false]);
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
