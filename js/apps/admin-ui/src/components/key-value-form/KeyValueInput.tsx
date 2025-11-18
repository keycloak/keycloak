import {
  ActionList,
  ActionListItem,
  Button,
  EmptyState,
  EmptyStateBody,
  EmptyStateFooter,
  Grid,
  GridItem,
  HelperText,
  HelperTextItem,
  TextInput,
} from "@patternfly/react-core";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { Fragment, FunctionComponent, PropsWithChildren } from "react";
import {
  FieldValues,
  useFieldArray,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

export type DefaultValue = {
  key: string;
  values?: string[];
  label: string;
};

type Field = {
  name: string;
  error: boolean;
};

type ValueField = Field & {
  keyValue: string;
};

type KeyValueInputProps = PropsWithChildren & {
  name: string;
  label?: string;
  isDisabled?: boolean;
  keyLabel?: string;
  valueLabel?: string;
  KeyComponent?: FunctionComponent<Field>;
  ValueComponent?: FunctionComponent<ValueField>;
};

export const KeyValueInput = ({
  name,
  label = "attributes",
  isDisabled = false,
  keyLabel = "key",
  valueLabel = "value",
  KeyComponent,
  ValueComponent,
}: KeyValueInputProps) => {
  const { t } = useTranslation();
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

  const values = useWatch<FieldValues>({
    name,
    control,
    defaultValue: [],
  });

  const getError = () => {
    return name.split(".").reduce((record: any, key) => record?.[key], errors);
  };

  return fields.length > 0 ? (
    <>
      <Grid hasGutter>
        <GridItem className="pf-v5-c-form__label" span={5}>
          <span className="pf-v5-c-form__label-text">{t(keyLabel)}</span>
        </GridItem>
        <GridItem className="pf-v5-c-form__label" span={7}>
          <span className="pf-v5-c-form__label-text">{t(valueLabel)}</span>
        </GridItem>
        {fields.map((attribute, index) => {
          const error = getError()?.[index];
          const keyError = !!error?.key;
          const valueErrorPresent = !!error?.value || !!error?.message;
          const valueError = error?.message || t(`${valueLabel}Error`);
          return (
            <Fragment key={attribute.id}>
              <GridItem span={5}>
                {KeyComponent ? (
                  <KeyComponent
                    name={`${name}.${index}.key`}
                    error={keyError}
                  />
                ) : (
                  <TextInput
                    placeholder={t(`${keyLabel}Placeholder`)}
                    aria-label={t(keyLabel)}
                    data-testid={`${name}-key`}
                    {...register(`${name}.${index}.key`, { required: true })}
                    validated={keyError ? "error" : "default"}
                    isRequired
                    isDisabled={isDisabled}
                  />
                )}
                {keyError && (
                  <HelperText>
                    <HelperTextItem variant="error">
                      {t(`${keyLabel}Error`)}
                    </HelperTextItem>
                  </HelperText>
                )}
              </GridItem>
              <GridItem span={5}>
                {ValueComponent ? (
                  <ValueComponent
                    name={`${name}.${index}.value`}
                    keyValue={values[index]?.key}
                    error={valueErrorPresent}
                  />
                ) : (
                  <TextInput
                    placeholder={t(`${valueLabel}Placeholder`)}
                    aria-label={t(valueLabel)}
                    data-testid={`${name}-value`}
                    {...register(`${name}.${index}.value`, { required: true })}
                    validated={valueErrorPresent ? "error" : "default"}
                    isRequired
                    isDisabled={isDisabled}
                  />
                )}
                {valueErrorPresent && (
                  <HelperText>
                    <HelperTextItem variant="error">
                      {valueError}
                    </HelperTextItem>
                  </HelperText>
                )}
              </GridItem>
              <GridItem span={2}>
                <Button
                  variant="link"
                  title={t("removeAttribute")}
                  onClick={() => remove(index)}
                  data-testid={`${name}-remove`}
                  isDisabled={isDisabled}
                >
                  <MinusCircleIcon />
                </Button>
              </GridItem>
            </Fragment>
          );
        })}
      </Grid>
      <ActionList>
        <ActionListItem>
          <Button
            data-testid={`${name}-add-row`}
            className="pf-v5-u-px-0 pf-v5-u-mt-sm"
            variant="link"
            icon={<PlusCircleIcon />}
            onClick={appendNew}
            isDisabled={isDisabled}
          >
            {t("addAttribute", { label })}
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
