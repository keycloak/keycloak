import {
  ActionList,
  ActionListItem,
  Button,
  EmptyState,
  EmptyStateBody,
  Grid,
  GridItem,
  HelperText,
  HelperTextItem,
  InputGroup,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Fragment } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";

type KeyValueInputProps = {
  name: string;
};

export const KeyValueInput = ({ name }: KeyValueInputProps) => {
  const { t } = useTranslation("common");
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext();

  const { fields, append, remove } = useFieldArray({
    control,
    name,
  });

  const appendNew = () => append({ key: "", value: "" });

  return fields.length > 0 ? (
    <>
      <Grid hasGutter>
        <GridItem className="pf-c-form__label" span={6}>
          <span className="pf-c-form__label-text">{t("key")}</span>
        </GridItem>
        <GridItem className="pf-c-form__label" span={6}>
          <span className="pf-c-form__label-text">{t("value")}</span>
        </GridItem>
        {fields.map((attribute, index) => {
          const keyError = !!(errors as any)[name]?.[index]?.key;
          const valueError = !!(errors as any)[name]?.[index]?.value;

          return (
            <Fragment key={attribute.id}>
              <GridItem span={6}>
                <KeycloakTextInput
                  placeholder={t("keyPlaceholder")}
                  aria-label={t("key")}
                  data-testid={`${name}-key`}
                  {...register(`${name}.${index}.key`, { required: true })}
                  validated={keyError ? "error" : "default"}
                  isRequired
                />
                {keyError && (
                  <HelperText>
                    <HelperTextItem variant="error">
                      {t("keyError")}
                    </HelperTextItem>
                  </HelperText>
                )}
              </GridItem>
              <GridItem span={6}>
                <InputGroup>
                  <KeycloakTextInput
                    placeholder={t("valuePlaceholder")}
                    aria-label={t("value")}
                    data-testid={`${name}-value`}
                    {...register(`${name}.${index}.value`, { required: true })}
                    validated={valueError ? "error" : "default"}
                    isRequired
                  />
                  <Button
                    variant="link"
                    title={t("removeAttribute")}
                    onClick={() => remove(index)}
                    data-testid={`${name}-remove`}
                  >
                    <MinusCircleIcon />
                  </Button>
                </InputGroup>
                {valueError && (
                  <HelperText>
                    <HelperTextItem variant="error">
                      {t("valueError")}
                    </HelperTextItem>
                  </HelperText>
                )}
              </GridItem>
            </Fragment>
          );
        })}
      </Grid>
      <ActionList>
        <ActionListItem>
          <Button
            data-testid={`${name}-add-row`}
            className="pf-u-px-0 pf-u-mt-sm"
            variant="link"
            icon={<PlusCircleIcon />}
            onClick={appendNew}
          >
            {t("addAttribute")}
          </Button>
        </ActionListItem>
      </ActionList>
    </>
  ) : (
    <EmptyState
      data-testid={`${name}-empty-state`}
      className="pf-u-p-0"
      variant="xs"
    >
      <EmptyStateBody>{t("missingAttributes")}</EmptyStateBody>
      <Button
        data-testid={`${name}-add-row`}
        variant="link"
        icon={<PlusCircleIcon />}
        isSmall
        onClick={appendNew}
      >
        {t("addAttribute")}
      </Button>
    </EmptyState>
  );
};
