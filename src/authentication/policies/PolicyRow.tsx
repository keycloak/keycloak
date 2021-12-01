import React from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  Button,
  FormGroup,
  NumberInput,
  Split,
  SplitItem,
  Switch,
  TextInput,
  ValidatedOptions,
} from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";

import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";

import "./policy-row.css";

type PolicyRowProps = {
  policy: PasswordPolicyTypeRepresentation;
  onRemove: (id?: string) => void;
};

export const PolicyRow = ({
  policy: { id, configType, defaultValue, displayName },
  onRemove,
}: PolicyRowProps) => {
  const { t } = useTranslation("authentication");
  const { control, register, errors } = useFormContext();
  return (
    <FormGroup
      label={displayName}
      fieldId={id!}
      isRequired
      helperTextInvalid={t("common:required")}
      validated={
        errors[id!] ? ValidatedOptions.error : ValidatedOptions.default
      }
    >
      <Split>
        <SplitItem isFilled>
          {configType && configType !== "int" && (
            <TextInput
              id={id}
              data-testid={id}
              ref={register({ required: true })}
              name={id}
              defaultValue={defaultValue}
              validated={
                errors[id!] ? ValidatedOptions.error : ValidatedOptions.default
              }
            />
          )}
          {configType === "int" && (
            <Controller
              name={id!}
              defaultValue={Number.parseInt(defaultValue || "0")}
              control={control}
              render={({ onChange, value }) => {
                const MIN_VALUE = 0;
                const setValue = (newValue: number) =>
                  onChange(Math.max(newValue, MIN_VALUE));

                return (
                  <NumberInput
                    id={id}
                    value={value}
                    min={MIN_VALUE}
                    onPlus={() => setValue(value + 1)}
                    onMinus={() => setValue(value - 1)}
                    onChange={(event) => {
                      const newValue = Number(event.currentTarget.value);
                      setValue(!isNaN(newValue) ? newValue : 0);
                    }}
                    className="keycloak__policies_authentication__number-field"
                  />
                );
              }}
            />
          )}
          {!configType && (
            <Switch
              id={id!}
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked
              isDisabled
            />
          )}
        </SplitItem>
        <SplitItem>
          <Button
            data-testid={`remove-${id}`}
            variant="link"
            className="keycloak__policies_authentication__minus-icon"
            onClick={() => onRemove(id)}
          >
            <MinusCircleIcon />
          </Button>
        </SplitItem>
      </Split>
    </FormGroup>
  );
};
