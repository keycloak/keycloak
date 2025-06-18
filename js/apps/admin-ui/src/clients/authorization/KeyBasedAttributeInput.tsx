import type ResourceRepresentation from "@keycloak/keycloak-admin-client/lib/defs/resourceRepresentation";
import { KeycloakSelect, SelectVariant } from "@keycloak/keycloak-ui-shared";
import { Button, SelectOption, TextInput } from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { camelCase } from "lodash-es";
import { useEffect, useMemo, useState } from "react";
import { Controller, useFieldArray, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { defaultContextAttributes } from "../utils";

import "./key-based-attribute-input.css";

export type AttributeType = {
  key?: string;
  name: string;
  custom?: boolean;
  values?: {
    [key: string]: string;
  }[];
};

type AttributeInputProps = {
  name: string;
  selectableValues?: AttributeType[];
  resources?: ResourceRepresentation[];
};

type ValueInputProps = {
  name: string;
  rowIndex: number;
  attribute: any;
  selectableValues?: AttributeType[];
  resources?: ResourceRepresentation[];
};

const ValueInput = ({
  name,
  rowIndex,
  attribute,
  selectableValues,
  resources,
}: ValueInputProps) => {
  const { t } = useTranslation();
  const { control, register, getValues } = useFormContext();
  const [isValueOpenArray, setIsValueOpenArray] = useState([false]);

  const toggleValueSelect = (rowIndex: number, open: boolean) => {
    const arr = [...isValueOpenArray];
    arr[rowIndex] = open;
    setIsValueOpenArray(arr);
  };

  const attributeValues = useMemo(() => {
    let values: AttributeType[] | undefined = [];

    if (selectableValues) {
      values = defaultContextAttributes.find(
        (attr) => attr.key === getValues().context?.[rowIndex]?.key,
      )?.values;
    }

    return values;
  }, [getValues]);

  const renderSelectOptionType = () => {
    const scopeValues = resources?.find(
      (resource) => resource.name === getValues().resources?.[rowIndex]?.key,
    )?.scopes;

    if (attributeValues?.length && !resources) {
      return attributeValues.map((attr) => (
        <SelectOption key={attr.key} value={attr.key}>
          {attr.name}
        </SelectOption>
      ));
    } else if (scopeValues?.length) {
      return scopeValues.map((scope) => (
        <SelectOption key={scope.name} value={scope.name}>
          {scope.name}
        </SelectOption>
      ));
    }
  };

  const getMessageBundleKey = (attributeName: string) =>
    camelCase(attributeName).replace(/\W/g, "");

  return (
    <Td>
      {resources || attributeValues?.length ? (
        <Controller
          name={`${name}.${rowIndex}.value`}
          defaultValue={[]}
          control={control}
          render={({ field }) => (
            <KeycloakSelect
              toggleId={`${attribute.id}-value`}
              className="kc-attribute-value-selectable"
              chipGroupProps={{
                numChips: 1,
                expandedText: t("hide"),
                collapsedText: t("showRemaining"),
              }}
              onToggle={(open) => toggleValueSelect(rowIndex, open)}
              isOpen={isValueOpenArray[rowIndex]}
              variant={SelectVariant.typeahead}
              typeAheadAriaLabel={t("selectOrTypeAKey")}
              placeholderText={t("selectOrTypeAKey")}
              selections={field.value}
              onSelect={(v) => {
                field.onChange(v);

                toggleValueSelect(rowIndex, false);
              }}
            >
              {renderSelectOptionType()}
            </KeycloakSelect>
          )}
        />
      ) : (
        <TextInput
          id={`${getMessageBundleKey(attribute.key)}-value`}
          className="value-input"
          defaultValue={attribute.value}
          data-testid="attribute-value-input"
          aria-label={t("value")}
          {...register(`${name}.${rowIndex}.value`)}
        />
      )}
    </Td>
  );
};

export const KeyBasedAttributeInput = ({
  name,
  selectableValues,
  resources,
}: AttributeInputProps) => {
  const { t } = useTranslation();
  const { control, watch } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control: control,
    name,
  });

  const [isKeyOpenArray, setIsKeyOpenArray] = useState([false]);
  const toggleKeySelect = (rowIndex: number, open: boolean) => {
    const arr = [...isKeyOpenArray];
    arr[rowIndex] = open;
    setIsKeyOpenArray(arr);
  };

  useEffect(() => {
    if (!fields.length) {
      append({ key: "", value: "" }, { shouldFocus: false });
    }
  }, [fields]);

  const watchLastValue = watch(`${name}.${fields.length - 1}.value`, "");

  return (
    <Table
      className="kc-attributes__table"
      aria-label="Role attribute keys and values"
      variant="compact"
    >
      <Thead>
        <Tr>
          <Th width={40}>{t("key")}</Th>
          <Th width={40}>{t("value")}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {fields.map((attribute, rowIndex) => (
          <Tr key={attribute.id} data-testid="attribute-row">
            <Td>
              <Controller
                name={`${name}.${rowIndex}.key`}
                defaultValue=""
                control={control}
                render={({ field }) => (
                  <KeycloakSelect
                    toggleId={`${name}.${rowIndex}.key`}
                    className="kc-attribute-key-selectable"
                    onToggle={(open) => toggleKeySelect(rowIndex, open)}
                    isOpen={isKeyOpenArray[rowIndex]}
                    variant={SelectVariant.typeahead}
                    typeAheadAriaLabel={t("selectOrTypeAKey")}
                    placeholderText={t("selectOrTypeAKey")}
                    selections={field.value}
                    onSelect={(v) => {
                      field.onChange(v.toString());

                      toggleKeySelect(rowIndex, false);
                    }}
                  >
                    {selectableValues?.map((attribute) => (
                      <SelectOption
                        selected={attribute.name === field.value}
                        key={attribute.key}
                        value={resources ? attribute.name : attribute.key}
                      >
                        {attribute.name}
                      </SelectOption>
                    ))}
                  </KeycloakSelect>
                )}
              />
            </Td>
            <ValueInput
              name={name}
              attribute={attribute}
              rowIndex={rowIndex}
              selectableValues={selectableValues}
              resources={resources}
            />
            <Td>
              <Button
                id={`${name}-minus-button-${rowIndex}`}
                variant="link"
                className="kc-attributes__minus-icon"
                onClick={() => remove(rowIndex)}
                aria-label={t("remove")}
              >
                <MinusCircleIcon />
              </Button>
            </Td>
          </Tr>
        ))}
        <Tr>
          <Td>
            <Button
              aria-label={t("addAttribute", { label: t("attribute") })}
              id={`${name}-plus-icon`}
              variant="link"
              className="kc-attributes__plus-icon"
              onClick={() => {
                append({ key: "", value: "" });
                setIsKeyOpenArray([...isKeyOpenArray, false]);
              }}
              icon={<PlusCircleIcon />}
              isDisabled={!watchLastValue}
              data-testid="attribute-add-row"
            >
              {t("addAttribute", { label: t("attribute") })}
            </Button>
          </Td>
        </Tr>
      </Tbody>
    </Table>
  );
};
