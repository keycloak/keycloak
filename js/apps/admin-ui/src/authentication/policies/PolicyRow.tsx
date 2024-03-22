import type PasswordPolicyTypeRepresentation from "@keycloak/keycloak-admin-client/lib/defs/passwordPolicyTypeRepresentation";
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
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormErrorText, HelpItem } from "@keycloak/keycloak-ui-shared";

import "./policy-row.css";

type PolicyRowProps = {
  policy: PasswordPolicyTypeRepresentation;
  onRemove: (id?: string) => void;
};

export const PolicyRow = ({
  policy: { id, configType, defaultValue, displayName },
  onRemove,
}: PolicyRowProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext();

  const error = errors[id!];

  return (
    <FormGroup
      label={displayName}
      fieldId={id!}
      isRequired
      labelIcon={
        <HelpItem
          helpText={t(`passwordPoliciesHelp.${id}`)}
          fieldLabelId={id!}
        />
      }
    >
      <Split>
        <SplitItem isFilled>
          {configType && configType !== "int" && (
            <TextInput
              id={id}
              data-testid={id}
              {...register(id!, { required: true })}
              defaultValue={defaultValue}
              validated={
                error ? ValidatedOptions.error : ValidatedOptions.default
              }
            />
          )}
          {configType === "int" && (
            <Controller
              name={id!}
              defaultValue={Number.parseInt(defaultValue || "0")}
              control={control}
              render={({ field }) => {
                const MIN_VALUE = 0;
                const setValue = (newValue: number) =>
                  field.onChange(Math.max(newValue, MIN_VALUE));
                const value = Number(field.value);

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
              label={t("on")}
              labelOff={t("off")}
              isChecked
              isDisabled
              aria-label={displayName}
            />
          )}
        </SplitItem>
        <SplitItem>
          <Button
            data-testid={`remove-${id}`}
            variant="link"
            className="keycloak__policies_authentication__minus-icon"
            onClick={() => onRemove(id)}
            aria-label={t("remove")}
          >
            <MinusCircleIcon />
          </Button>
        </SplitItem>
      </Split>
      {error && <FormErrorText message={t("required")} />}
    </FormGroup>
  );
};
