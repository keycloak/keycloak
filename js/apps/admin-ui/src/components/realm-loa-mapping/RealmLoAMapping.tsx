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
import { Fragment, PropsWithChildren } from "react";
import { useFieldArray, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

type RealmLoAMappingProps = PropsWithChildren & {
  name: string;
  label?: string;
  uri: boolean;
};

export type RealmLoAMappingType = { acr: string; uri?: string; loa: string };

export const RealmLoAMapping = ({
  name,
  label = "attributes",
  uri = false,
}: RealmLoAMappingProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext();

  const spanAcr = uri ? 4 : 5;
  const spanLoA = uri ? 2 : 5;

  const { fields, append, remove } = useFieldArray({
    control,
    name,
  });

  const appendNew = () => append({ acr: "", uri: "", loa: "" });

  const getError = () => {
    return name.split(".").reduce((record: any, key) => record?.[key], errors);
  };

  return fields.length > 0 ? (
    <>
      <Grid hasGutter>
        <GridItem className="pf-v5-c-form__label" span={spanAcr}>
          <span className="pf-v5-c-form__label-text">{t("acr")}</span>
        </GridItem>
        {uri && (
          <GridItem className="pf-v5-c-form__label" span={spanAcr}>
            <span className="pf-v5-c-form__label-text">{t("uri")}</span>
          </GridItem>
        )}
        <GridItem className="pf-v5-c-form__label" span={spanLoA}>
          <span className="pf-v5-c-form__label-text">{t("loa")}</span>
        </GridItem>
        {fields.map((attribute, index) => {
          const error = getError()?.[index];
          return (
            <Fragment key={attribute.id}>
              <GridItem span={spanAcr}>
                <TextInput
                  placeholder={t("acrPlaceholder")}
                  aria-label={t("acr")}
                  data-testid={`${name}-acr`}
                  {...register(`${name}.${index}.acr`, { required: true })}
                  validated={error?.acr ? "error" : "default"}
                  isRequired
                />
                {error?.acr && (
                  <HelperText>
                    <HelperTextItem variant="error">
                      {t("acrError")}
                    </HelperTextItem>
                  </HelperText>
                )}
              </GridItem>
              {uri && (
                <GridItem span={spanAcr}>
                  <TextInput
                    placeholder={t("uriPlaceholder")}
                    aria-label={t("uri")}
                    data-testid={`${name}-uri`}
                    {...register(`${name}.${index}.uri`, {
                      // some validation for URI in JS????
                    })}
                    validated={error?.uri ? "error" : "default"}
                    isRequired={false}
                  />
                  {error?.uri && (
                    <HelperText>
                      <HelperTextItem variant="error">
                        {t("uriError")}
                      </HelperTextItem>
                    </HelperText>
                  )}
                </GridItem>
              )}
              <GridItem span={spanLoA}>
                <TextInput
                  placeholder={t("loaPlaceholder")}
                  aria-label={t("loa")}
                  data-testid={`${name}-loa`}
                  {...register(`${name}.${index}.loa`, {
                    required: true,
                    validate: (v: string) => Number.isInteger(parseInt(v)),
                  })}
                  validated={error?.loa ? "error" : "default"}
                  isRequired
                />
                {error?.loa && (
                  <HelperText>
                    <HelperTextItem variant="error">
                      {t("loaError")}
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
        >
          {t("addAttribute", { label })}
        </Button>
      </EmptyStateFooter>
    </EmptyState>
  );
};
