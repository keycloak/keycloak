import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import {
  PageSection,
  FormGroup,
  Radio,
  Select,
  SelectVariant,
  SelectOption,
  NumberInput,
  ActionGroup,
  Button,
  TextInput,
  ButtonVariant,
  AlertVariant,
} from "@patternfly/react-core";

import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import useToggle from "../../utils/useToggle";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useAlerts } from "../../components/alert/Alerts";

import "./otp-policy.css";

const POLICY_TYPES = ["totp", "hotp"] as const;
const OTP_HASH_ALGORITHMS = ["SHA1", "SHA256", "SHA512"] as const;
const NUMBER_OF_DIGITS = [6, 8] as const;

export const OtpPolicy = () => {
  const { t } = useTranslation("authentication");
  const {
    control,
    errors,
    reset,
    handleSubmit,
    formState: { isDirty },
  } = useForm({ mode: "onChange" });
  const adminClient = useAdminClient();
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();

  const [open, toggle] = useToggle();
  const [realm, setRealm] = useState<RealmRepresentation>();

  const otpType = useWatch<typeof POLICY_TYPES[number]>({
    name: "otpPolicyType",
    control,
    defaultValue: POLICY_TYPES[0],
  });

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      setRealm(realm);
      reset({ ...realm });
    },
    []
  );

  const save = async (realm: RealmRepresentation) => {
    try {
      await adminClient.realms.update({ realm: realmName }, realm);
      const updatedRealm = await adminClient.realms.findOne({
        realm: realmName,
      });
      setRealm(updatedRealm);
      reset({ ...updatedRealm });
      addAlert(t("updateOtpSuccess"), AlertVariant.success);
    } catch (error) {
      addError("authentication:updateOtpError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(save)}
        className="keycloak__otp_policies_authentication__form"
      >
        <FormGroup
          label={t("otpType")}
          labelIcon={
            <HelpItem
              helpText="authentication-help:otpType"
              fieldLabelId="authentication:otpType"
            />
          }
          fieldId="otpType"
          hasNoPaddingTop
        >
          <Controller
            name="otpPolicyType"
            data-testid="otpPolicyType"
            defaultValue={POLICY_TYPES[0]}
            control={control}
            render={({ onChange, value }) => (
              <>
                {POLICY_TYPES.map((type) => (
                  <Radio
                    id={type}
                    key={type}
                    data-testid={type}
                    isChecked={value === type}
                    name="otpPolicyType"
                    onChange={() => onChange(type)}
                    label={t(`policyType.${type}`)}
                    className="keycloak__otp_policies_authentication__policy-type"
                  />
                ))}
              </>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("otpHashAlgorithm")}
          labelIcon={
            <HelpItem
              helpText="authentication-help:otpHashAlgorithm"
              fieldLabelId="authentication:otpHashAlgorithm"
            />
          }
          fieldId="otpHashAlgorithm"
        >
          <Controller
            name="otpPolicyAlgorithm"
            defaultValue={`Hmac${OTP_HASH_ALGORITHMS[0]}`}
            control={control}
            render={({ onChange, value }) => (
              <Select
                toggleId="otpHashAlgorithm"
                onToggle={toggle}
                onSelect={(_, value) => {
                  onChange(value.toString());
                  toggle();
                }}
                selections={value}
                variant={SelectVariant.single}
                aria-label={t("otpHashAlgorithm")}
                isOpen={open}
              >
                {OTP_HASH_ALGORITHMS.map((type) => (
                  <SelectOption
                    selected={`Hmac${type}` === value}
                    key={type}
                    value={`Hmac${type}`}
                  >
                    {type}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("otpPolicyDigits")}
          labelIcon={
            <HelpItem
              helpText="authentication-help:otpPolicyDigits"
              fieldLabelId="authentication:otpPolicyDigits"
            />
          }
          fieldId="otpPolicyDigits"
          hasNoPaddingTop
        >
          <Controller
            name="otpPolicyDigits"
            data-testid="otpPolicyDigits"
            defaultValue={NUMBER_OF_DIGITS[0]}
            control={control}
            render={({ onChange, value }) => (
              <>
                {NUMBER_OF_DIGITS.map((type) => (
                  <Radio
                    id={`digit-${type}`}
                    key={type}
                    data-testid={`digit-${type}`}
                    isChecked={value === type}
                    name="otpPolicyDigits"
                    onChange={() => onChange(type)}
                    label={type}
                    className="keycloak__otp_policies_authentication__number-of-digits"
                  />
                ))}
              </>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("lookAhead")}
          labelIcon={
            <HelpItem
              helpText="authentication-help:lookAhead"
              fieldLabelId="authentication:lookAhead"
            />
          }
          fieldId="lookAhead"
        >
          <Controller
            name="otpPolicyLookAheadWindow"
            defaultValue={1}
            control={control}
            render={({ onChange, value }) => {
              const MIN_VALUE = 0;
              const setValue = (newValue: number) =>
                onChange(Math.max(newValue, MIN_VALUE));

              return (
                <NumberInput
                  id="lookAhead"
                  value={value}
                  min={MIN_VALUE}
                  onPlus={() => setValue(value + 1)}
                  onMinus={() => setValue(value - 1)}
                  onChange={(event) => {
                    const newValue = Number(event.currentTarget.value);
                    setValue(!isNaN(newValue) ? newValue : 0);
                  }}
                />
              );
            }}
          />
        </FormGroup>
        {otpType === POLICY_TYPES[0] && (
          <FormGroup
            label={t("otpPolicyPeriod")}
            fieldId="otpPolicyPeriod"
            helperTextInvalid={t("otpPolicyPeriodErrorHint")}
            validated={errors.otpPolicyPeriod ? "error" : "default"}
            labelIcon={
              <HelpItem
                helpText="authentication-help:otpPolicyPeriod"
                fieldLabelId="authentication:otpPolicyPeriod"
              />
            }
          >
            <Controller
              name="otpPolicyPeriod"
              defaultValue={30}
              control={control}
              rules={{ min: 1, max: 120 }}
              render={({ onChange, value }) => (
                <TimeSelector
                  data-testid="otpPolicyPeriod"
                  aria-label={t("otpPolicyPeriod")}
                  value={value}
                  onChange={onChange}
                  units={["seconds", "minutes"]}
                  validated={errors.otpPolicyPeriod ? "error" : "default"}
                />
              )}
            />
          </FormGroup>
        )}
        {otpType === POLICY_TYPES[1] && (
          <FormGroup
            label={t("initialCounter")}
            fieldId="initialCounter"
            helperTextInvalid={t("initialCounterErrorHint")}
            validated={errors.otpPolicyInitialCounter ? "error" : "default"}
            labelIcon={
              <HelpItem
                helpText="authentication-help:initialCounter"
                fieldLabelId="authentication:initialCounter"
              />
            }
          >
            <Controller
              name="otpPolicyInitialCounter"
              defaultValue={30}
              control={control}
              rules={{ min: 1, max: 120 }}
              render={({ onChange, value }) => {
                const MIN_VALUE = 1;
                const setValue = (newValue: number) =>
                  onChange(Math.max(newValue, MIN_VALUE));

                return (
                  <NumberInput
                    id="initialCounter"
                    value={value}
                    min={MIN_VALUE}
                    onPlus={() => setValue(value + 1)}
                    onMinus={() => setValue(value - 1)}
                    onChange={(event) => {
                      const newValue = Number(event.currentTarget.value);
                      setValue(!isNaN(newValue) ? newValue : 30);
                    }}
                  />
                );
              }}
            />
          </FormGroup>
        )}
        <FormGroup
          label={t("supportedActions")}
          fieldId="supportedActions"
          labelIcon={
            <HelpItem
              helpText="authentication-help:supportedActions"
              fieldLabelId="authentication:supportedActions"
            />
          }
        >
          <TextInput
            id="supportedActions"
            data-testid="supportedActions"
            isReadOnly
            value={realm?.otpSupportedApplications?.join(", ")}
          />
        </FormGroup>
        <ActionGroup>
          <Button
            data-testid="save"
            variant="primary"
            type="submit"
            isDisabled={!isDirty}
          >
            {t("common:save")}
          </Button>
          <Button
            data-testid="reload"
            variant={ButtonVariant.link}
            onClick={() => reset({ ...realm })}
          >
            {t("common:reload")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
