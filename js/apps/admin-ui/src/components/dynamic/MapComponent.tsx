import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
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
  SelectOption,
  TextInput,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { KeyValueType } from "../key-value-form/key-value-convert";
import type { ComponentProps } from "./components";

export const MapComponent = ({
  name,
  label,
  helpText,
  required,
  isDisabled,
  defaultValue,
  options,
  convertToName,
}: ComponentProps) => {
  const { t } = useTranslation();

  const { watch, setValue } = useFormContext();
  const [map, setMap] = useState<KeyValueType[]>([]);
  const fieldName = convertToName(name!);
  const value = watch(fieldName, defaultValue || "[]");
  useEffect(() => {
    const values: KeyValueType[] = JSON.parse(value ? value : "[]");
    setMap(values);
  }, [value]);

  const appendNew = () => setMap([...map, { key: "", value: "" }]);

  const update = (val = map) => {
    const v = val.filter((e) => e.key !== "");
    setValue(fieldName, v.length > 0 ? JSON.stringify(v) : "");
  };

  const updateKey = (index: number, key: string) => {
    return updateEntry(index, { ...map[index], key });
  };

  const updateValue = (index: number, value: string) => {
    return updateEntry(index, { ...map[index], value });
  };

  const updateEntry = (index: number, entry: KeyValueType) => {
    const newMap = [...map.slice(0, index), entry, ...map.slice(index + 1)];
    setMap(newMap);
    return newMap;
  };

  const remove = (index: number) => {
    const value = [...map.slice(0, index), ...map.slice(index + 1)];
    setMap(value);
    update(value);
  };

  const [open, setOpen] = useState(-1);

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={`${label}`} />}
      fieldId={name!}
      isRequired={required}
    >
      {map.length !== 0 ? (
        <>
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
              <Flex key={index} data-testid="row">
                <FlexItem grow={{ default: "grow" }}>
                  {options ? (
                    <KeycloakSelect
                      variant={SelectVariant.single}
                      onToggle={(v) => setOpen(v ? index : -1)}
                      selections={attribute.key}
                      data-testid={`${fieldName}.${index}.key`}
                      onSelect={(value) => {
                        update(updateKey(index, value.toString()));
                      }}
                      isOpen={open === index}
                      className={
                        attribute.key && !options.includes(attribute.key)
                          ? "pf-m-danger"
                          : ""
                      }
                    >
                      <SelectOption value="">{t("choose")}</SelectOption>
                      {options.map((option, index) => (
                        <SelectOption key={index} value={option}>
                          {option}
                        </SelectOption>
                      ))}
                    </KeycloakSelect>
                  ) : (
                    <TextInput
                      name={`${fieldName}.${index}.key`}
                      placeholder={t("keyPlaceholder")}
                      aria-label={t("key")}
                      value={attribute.key}
                      data-testid={`${fieldName}.${index}.key`}
                      onChange={(_event, value) => updateKey(index, value)}
                      onBlur={() => update()}
                    />
                  )}
                </FlexItem>
                <FlexItem
                  grow={{ default: "grow" }}
                  spacer={{ default: "spacerNone" }}
                >
                  <TextInput
                    name={`${fieldName}.${index}.value`}
                    placeholder={t("valuePlaceholder")}
                    aria-label={t("value")}
                    value={attribute.value}
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
                isDisabled={isDisabled || map.some((e) => e.key === "")}
              >
                {t("addAttribute", { label: t(label!) })}
              </Button>
            </ActionListItem>
          </ActionList>
        </>
      ) : (
        <EmptyState
          data-testid={`${name}-empty-state`}
          className="pf-v5-u-p-0"
          variant="xs"
        >
          <EmptyStateBody>
            {t("missingAttributes", { label: t(label!) })}
          </EmptyStateBody>
          <EmptyStateFooter>
            <Button
              data-testid={`${name}-add-row`}
              variant="link"
              icon={<PlusCircleIcon />}
              size="sm"
              onClick={appendNew}
              isDisabled={isDisabled}
            >
              {t("addAttribute", { label: t(label!) })}
            </Button>
          </EmptyStateFooter>
        </EmptyState>
      )}
    </FormGroup>
  );
};
