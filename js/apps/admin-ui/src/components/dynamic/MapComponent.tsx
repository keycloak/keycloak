import { HelpItem, generateId } from "@keycloak/keycloak-ui-shared";
import {
  ActionList,
  ActionListItem,
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateFooter,
  Flex,
  FlexItem,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeyValueType } from "../key-value-form/key-value-convert";
import type { ComponentProps } from "./components";

type IdKeyValueType = KeyValueType & {
  id: number;
};

export const MapComponent = ({
  name,
  label,
  helpText,
  required,
  isDisabled,
  defaultValue,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();

  const { getValues, setValue, register } = useFormContext();
  const [map, setMap] = useState<IdKeyValueType[]>([]);
  const fieldName = convertToName(name!);

  useEffect(() => {
    register(fieldName);
    const values: KeyValueType[] = JSON.parse(
      getValues(fieldName) || defaultValue || "[]",
    );
    setMap(values.map((value) => ({ ...value, id: generateId() })));
  }, []);

  const appendNew = () =>
    setMap([...map, { key: "", value: "", id: generateId() }]);

  const update = (val = map) => {
    setValue(
      fieldName,
      JSON.stringify(
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        val.filter((e) => e.key !== "").map(({ id, ...entry }) => entry),
      ),
    );
  };

  const updateKey = (index: number, key: string) => {
    updateEntry(index, { ...map[index], key });
  };

  const updateValue = (index: number, value: string) => {
    updateEntry(index, { ...map[index], value });
  };

  const updateEntry = (index: number, entry: IdKeyValueType) =>
    setMap([...map.slice(0, index), entry, ...map.slice(index + 1)]);

  const remove = (index: number) => {
    const value = [...map.slice(0, index), ...map.slice(index + 1)];
    setMap(value);
    update(value);
  };

  return map.length !== 0 ? (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      <Flex direction={{ default: "column" }}>
        <Flex>
          <FlexItem
            grow={{ default: "grow" }}
            spacer={{ default: "spacerNone" }}
          >
            <strong>{t("key")}</strong>
          </FlexItem>
          <FlexItem grow={{ default: "grow" }}>
            <strong>{t("value")}</strong>
          </FlexItem>
        </Flex>
        {map.map((attribute, index) => (
          <Flex key={attribute.id} data-testid="row">
            <FlexItem grow={{ default: "grow" }}>
              <TextInput
                name={`${fieldName}.${index}.key`}
                placeholder={t("keyPlaceholder")}
                aria-label={t("key")}
                defaultValue={attribute.key}
                data-testid={`${fieldName}.${index}.key`}
                onChange={(_event, value) => updateKey(index, value)}
                onBlur={() => update()}
              />
            </FlexItem>
            <FlexItem
              grow={{ default: "grow" }}
              spacer={{ default: "spacerNone" }}
            >
              <TextInput
                name={`${fieldName}.${index}.value`}
                placeholder={t("valuePlaceholder")}
                aria-label={t("value")}
                defaultValue={attribute.value}
                data-testid={`${fieldName}.${index}.value`}
                onChange={(_event, value) => updateValue(index, value)}
                onBlur={() => update()}
              />
            </FlexItem>
            <FlexItem>
              <Button
                variant="link"
                title={t("removeAttribute")}
                isDisabled={isDisabled}
                onClick={() => remove(index)}
                data-testid={`${fieldName}.${index}.remove`}
              >
                <MinusCircleIcon />
              </Button>
            </FlexItem>
          </Flex>
        ))}
      </Flex>
      <ActionList>
        <ActionListItem>
          <Button
            data-testid={`${fieldName}-add-row`}
            className="pf-v5-u-px-0 pf-v5-u-mt-sm"
            variant="link"
            icon={<PlusCircleIcon />}
            onClick={() => appendNew()}
          >
            {t("addAttribute", { label })}
          </Button>
        </ActionListItem>
      </ActionList>
    </FormGroup>
  ) : (
    <EmptyState
      data-testid={`${name}-empty-state`}
      className="pf-v5-u-p-0"
      variant="xs"
    >
      <EmptyStateBody>{t("missingAttributes", { label })}</EmptyStateBody>
      <EmptyStateFooter>
        <Button
          data-testid={`${name}-add-row`}
          variant="link"
          icon={<PlusCircleIcon />}
          size="sm"
          onClick={appendNew}
          isDisabled={isDisabled}
        >
          {t("addAttribute", { label })}
        </Button>
      </EmptyStateFooter>
    </EmptyState>
  );
};
