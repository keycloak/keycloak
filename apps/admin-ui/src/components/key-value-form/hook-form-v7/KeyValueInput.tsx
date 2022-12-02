import {
  ActionList,
  ActionListItem,
  Button,
  Flex,
  FlexItem,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useEffect } from "react";
import { useFieldArray, useFormContext, useWatch } from "react-hook-form-v7";
import { useTranslation } from "react-i18next";

import { KeycloakTextInput } from "../../keycloak-text-input/KeycloakTextInput";
import { KeyValueType } from "../key-value-convert";

type KeyValueInputProps = {
  name: string;
};

export const KeyValueInput = ({ name }: KeyValueInputProps) => {
  const { t } = useTranslation("common");
  const { control, register } = useFormContext();
  const { fields, append, remove } = useFieldArray({
    control,
    name,
  });

  const watchFields = useWatch({
    control,
    name,
    defaultValue: [{ key: "", value: "" }],
  });

  const isValid =
    Array.isArray(watchFields) &&
    watchFields.every(
      ({ key, value }: KeyValueType) =>
        // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
        key?.trim().length !== 0 && value?.trim().length !== 0
    );

  useEffect(() => {
    if (!fields.length) {
      append({ key: "", value: "" }, { shouldFocus: false });
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
                placeholder={t("keyPlaceholder")}
                aria-label={t("key")}
                defaultValue=""
                data-testid={`${name}[${index}].key`}
                {...register(`${name}[${index}].key`)}
              />
            </FlexItem>
            <FlexItem
              grow={{ default: "grow" }}
              spacer={{ default: "spacerNone" }}
            >
              <KeycloakTextInput
                placeholder={t("valuePlaceholder")}
                aria-label={t("value")}
                defaultValue=""
                data-testid={`${name}[${index}].value`}
                {...register(`${name}[${index}].value`)}
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
