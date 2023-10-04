import {
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Grid,
  GridItem,
  ActionList,
  ActionListItem,
  EmptyState,
  EmptyStateBody,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
import { sortBy } from "lodash-es";
import { useState, Fragment } from "react";
import { Controller, useFormContext, useFieldArray } from "react-hook-form";
import { MinusCircleIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";

import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import { KeycloakTextInput } from "../keycloak-text-input/KeycloakTextInput";
import { convertAttributeNameToForm } from "../../util";

export const AcrFlowMapping = () => {
  const { t } = useTranslation();
  const [flows, setFlows] = useState<JSX.Element[]>([]);
  const [selectOpen] = useState({} as Record<string, boolean>);
  const [key, setKey] = useState(0);

  const name: string = convertAttributeNameToForm(
    "attributes.acr.authflow.map",
  );
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext();

  const { fields, append, remove } = useFieldArray({
    shouldUnregister: true,
    control,
    name,
  });

  const appendNew = () => append({ key: "", value: "" });

  const refresh = () => setKey(key + 1);

  const toggle = (k: string) => {
    const state = selectOpen[k] ? true : false;
    selectOpen[k] = !state;
    refresh();
  };

  useFetch(
    () => adminClient.authenticationManagement.getFlows(),
    (flows) => {
      let filteredFlows = [
        ...flows.filter((flow) => flow.providerId !== "client-flow"),
      ];
      filteredFlows = sortBy(filteredFlows, [(f) => f.alias]);
      setFlows([
        <SelectOption key="empty" value="">
          {t("choose")}
        </SelectOption>,
        ...filteredFlows.map((flow) => (
          <SelectOption key={flow.id} value={flow.id}>
            {flow.alias}
          </SelectOption>
        )),
      ]);
    },
    [],
  );

  return (
    <FormGroup
      label={t("acrFlowMapping.title")}
      fieldId="acrFlowMapping"
      labelIcon={
        <HelpItem
          helpText={t("acrFlowMapping.help")}
          fieldLabelId="acrFlowMapping"
        />
      }
    >
      {fields.length > 0 ? (
        <>
          <Grid hasGutter>
            <GridItem className="pf-c-form__label" span={5}>
              <span className="pf-c-form__label-text">
                {t("acrFlowMapping.key.header")}
              </span>
            </GridItem>
            <GridItem className="pf-c-form__label" span={7}>
              <span className="pf-c-form__label-text">
                {t("acrFlowMapping.value.header")}
              </span>
            </GridItem>
            {fields.map((attribute, index) => {
              const keyError = !!(errors as any).attributes?.[
                name.replace("attributes.", "")
              ]?.[index]?.key;
              const valueError = !!(errors as any).attributes?.[
                name.replace("attributes.", "")
              ]?.[index]?.value;

              return (
                <Fragment key={attribute.id}>
                  <GridItem span={5}>
                    <KeycloakTextInput
                      placeholder={t("acrFlowMapping.key.placeholder")}
                      data-testid={`${name}-key`}
                      {...register(`${name}.${index}.key`, { required: true })}
                      validated={keyError ? "error" : "default"}
                      isRequired
                    />
                    {keyError && (
                      <HelperText>
                        <HelperTextItem variant="error">
                          {t("acrFlowMapping.key.error")}
                        </HelperTextItem>
                      </HelperText>
                    )}
                  </GridItem>
                  <GridItem span={5}>
                    <Controller
                      name={`${name}.${index}.value`}
                      control={control}
                      render={({ field }) => (
                        <Select
                          placeholderText={t(
                            "acrFlowMapping.value.placeholder",
                          )}
                          toggleId={`${name}.${index}.value`}
                          variant={SelectVariant.single}
                          onToggle={() => toggle(`${name}.${index}.value`)}
                          isOpen={selectOpen[`${name}.${index}.value`]}
                          onSelect={(_, value) => {
                            field.onChange(value);
                            toggle(`${name}.${index}.value`);
                          }}
                          selections={[field.value]}
                          {...register(`${name}.${index}.value`, {
                            required: true,
                          })}
                          validated={valueError ? "error" : "default"}
                        >
                          {flows}
                        </Select>
                      )}
                    />

                    {valueError && (
                      <HelperText>
                        <HelperTextItem variant="error">
                          {t("acrFlowMapping.value.error")}
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
                className="pf-u-px-0 pf-u-mt-sm"
                variant="link"
                icon={<PlusCircleIcon />}
                onClick={appendNew}
              >
                {t("acrFlowMapping.add")}
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
          <EmptyStateBody>{t("acrFlowMapping.empty")}</EmptyStateBody>
          <Button
            data-testid={`${name}-add-row`}
            variant="link"
            icon={<PlusCircleIcon />}
            isSmall
            onClick={appendNew}
          >
            {t("acrFlowMapping.add")}
          </Button>
        </EmptyState>
      )}
    </FormGroup>
  );
};
