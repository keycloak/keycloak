import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form";
import {
  ActionList,
  ActionListItem,
  Button,
  Flex,
  FlexItem,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";

import type { KeyValueType } from "./key-value-convert";
import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";

type KeyValueInputProps = {
  name: string;
};

export const KeyValueInput = ({ name }: KeyValueInputProps) => {
  const { t } = useTranslation("common");
  const { control, register } = useFormContext();
  const { fields, append, remove } = useFieldArray<KeyValueType>({
    control: control,
    name,
  });

  const watchFields = useWatch<KeyValueType[]>({
    control,
    name,
    defaultValue: [],
  });

  const isValid = watchFields.every(
    ({ key, value }) =>
      key && key.trim().length !== 0 && value && value.trim().length !== 0
  );

  useEffect(() => {
    if (!fields.length) {
      append({ key: "", value: "" }, false);
    }
  }, [fields]);

  return (
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
        {fields.map((attribute, index) => (
          <Flex key={attribute.id} data-testid="row">
            <FlexItem grow={{ default: "grow" }}>
              <KeycloakTextInput
                name={`${name}[${index}].key`}
                ref={register()}
                placeholder={t("keyPlaceholder")}
                aria-label={t("key")}
                defaultValue={attribute.key}
                data-testid={`${name}[${index}].key`}
              />
            </FlexItem>
            <FlexItem
              grow={{ default: "grow" }}
              spacer={{ default: "spacerNone" }}
            >
              <KeycloakTextInput
                name={`${name}[${index}].value`}
                ref={register()}
                placeholder={t("valuePlaceholder")}
                aria-label={t("value")}
                defaultValue={attribute.value}
                data-testid={`${name}[${index}].value`}
              />
            </FlexItem>
            <FlexItem>
              <Button
                variant="link"
                title={t("removeAttribute")}
                isDisabled={watchFields.length === 1}
                onClick={() => remove(index)}
                data-testid={`${name}[${index}].remove`}
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
            data-testid={`${name}-add-row`}
            className="pf-u-px-0 pf-u-mt-sm"
            variant="link"
            icon={<PlusCircleIcon />}
            isDisabled={!isValid}
            onClick={() => append({ key: "", value: "" })}
          >
            {t("addAttribute")}
          </Button>
        </ActionListItem>
      </ActionList>
    </>
  );
};
