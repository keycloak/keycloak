import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  FormGroup,
  NumberInput,
  PageSection,
  Radio,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  ValidatedOptions,
} from "@patternfly/react-core";
import { useEffect, useMemo } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useRealm } from "../../context/realm-context/RealmContext";
import useLocaleSort from "../../utils/useLocaleSort";
import useToggle from "../../utils/useToggle";

import "./otp-policy.css";

const POLICY_TYPES = ["totp", "hotp"] as const;
const OTP_HASH_ALGORITHMS = ["SHA1", "SHA256", "SHA512"] as const;
const NUMBER_OF_DIGITS = [6, 8] as const;

type OtpPolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
};

type FormFields = Omit<
  RealmRepresentation,
  "clients" | "components" | "groups" | "users" | "federatedUsers"
>;

export const OtpPolicy = ({ realm, realmUpdated }: OtpPolicyProps) => {
  const { t } = useTranslation();
  const {
    control,
    reset,
    handleSubmit,
    formState: { isValid, isDirty, errors },
  } = useForm<FormFields>({ mode: "onChange", defaultValues: realm });
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();
  const localeSort = useLocaleSort();
  const [open, toggle] = useToggle();

  const otpType = useWatch({
    name: "otpPolicyType",
    control,
    defaultValue: POLICY_TYPES[0],
  });

  const setupForm = (formValues: FormFields) => reset(formValues);

  useEffect(() => setupForm(realm), []);

  const supportedApplications = useMemo(() => {
    const labels = (realm.otpSupportedApplications ?? []).map((key) =>
      t(`otpSupportedApplications.${key}`),
    );

    return localeSort(labels, (label) => label);
  }, [realm.otpSupportedApplications]);

  const onSubmit = async (formValues: FormFields) => {
    try {
      await adminClient.realms.update({ realm: realmName }, formValues);
      const updatedRealm = await adminClient.realms.findOne({
        realm: realmName,
      });
      realmUpdated(updatedRealm!);
      setupForm(updatedRealm!);
      addAlert(t("updateOtpSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateOtpError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(onSubmit)}
        className="keycloak__otp_policies_authentication__form"
      >
        <FormGroup
          label={t("otpType")}
          labelIcon={
            <HelpItem helpText={t("otpTypeHelp")} fieldLabelId="otpType" />
          }
          hasNoPaddingTop
        >
          <Controller
            name="otpPolicyType"
            data-testid="otpPolicyType"
            defaultValue={POLICY_TYPES[0]}
            control={control}
            render={({ field }) => (
              <>
                {POLICY_TYPES.map((type) => (
                  <Radio
                    key={type}
                    id={type}
                    data-testid={type}
                    isChecked={field.value === type}
                    name="otpPolicyType"
                    onChange={() => field.onChange(type)}
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
              helpText={t("otpHashAlgorithmHelp")}
              fieldLabelId="otpHashAlgorithm"
            />
          }
          fieldId="otpHashAlgorithm"
        >
          <Controller
            name="otpPolicyAlgorithm"
            defaultValue={`Hmac${OTP_HASH_ALGORITHMS[0]}`}
            control={control}
            render={({ field }) => (
              <Select
                toggleId="otpHashAlgorithm"
                onToggle={toggle}
                onSelect={(_, value) => {
                  field.onChange(value.toString());
                  toggle();
                }}
                selections={field.value}
                variant={SelectVariant.single}
                isOpen={open}
              >
                {OTP_HASH_ALGORITHMS.map((type) => (
                  <SelectOption
                    key={type}
                    selected={`Hmac${type}` === field.value}
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
              helpText={t("otpPolicyDigitsHelp")}
              fieldLabelId="otpPolicyDigits"
            />
          }
          hasNoPaddingTop
        >
          <Controller
            name="otpPolicyDigits"
            data-testid="otpPolicyDigits"
            defaultValue={NUMBER_OF_DIGITS[0]}
            control={control}
            render={({ field }) => (
              <>
                {NUMBER_OF_DIGITS.map((type) => (
                  <Radio
                    key={type}
                    id={`digit-${type}`}
                    data-testid={`digit-${type}`}
                    isChecked={field.value === type}
                    name="otpPolicyDigits"
                    onChange={() => field.onChange(type)}
                    label={type}
                    className="keycloak__otp_policies_authentication__number-of-digits"
                  />
                ))}
              </>
            )}
          />
        </FormGroup>
        <FormGroup
          label={t("lookAround")}
          labelIcon={
            <HelpItem
              helpText={t("lookAroundHelp")}
              fieldLabelId="lookAround"
            />
          }
          fieldId="lookAround"
        >
          <Controller
            name="otpPolicyLookAheadWindow"
            defaultValue={1}
            control={control}
            render={({ field }) => {
              const MIN_VALUE = 0;
              const value = field.value ?? 1;
              const setValue = (newValue: number) =>
                field.onChange(Math.max(newValue, MIN_VALUE));

              return (
                <NumberInput
                  id="lookAround"
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
            validated={
              errors.otpPolicyPeriod
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            labelIcon={
              <HelpItem
                helpText={t("otpPolicyPeriodHelp")}
                fieldLabelId="otpPolicyPeriod"
              />
            }
          >
            <Controller
              name="otpPolicyPeriod"
              defaultValue={30}
              control={control}
              rules={{ min: 1, max: 120 }}
              render={({ field }) => {
                const value = field.value ?? 30;

                return (
                  <TimeSelector
                    id="otpPolicyPeriod"
                    data-testid="otpPolicyPeriod"
                    value={value}
                    onChange={field.onChange}
                    units={["second", "minute"]}
                    validated={
                      errors.otpPolicyPeriod
                        ? ValidatedOptions.error
                        : ValidatedOptions.default
                    }
                  />
                );
              }}
            />
          </FormGroup>
        )}
        {otpType === POLICY_TYPES[1] && (
          <FormGroup
            label={t("initialCounter")}
            fieldId="initialCounter"
            helperTextInvalid={t("initialCounterErrorHint")}
            validated={
              errors.otpPolicyInitialCounter
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
            labelIcon={
              <HelpItem
                helpText={t("initialCounterHelp")}
                fieldLabelId="initialCounter"
              />
            }
          >
            <Controller
              name="otpPolicyInitialCounter"
              defaultValue={30}
              control={control}
              rules={{ min: 1, max: 120 }}
              render={({ field }) => {
                const MIN_VALUE = 1;
                const value = field.value ?? 30;
                const setValue = (newValue: number) =>
                  field.onChange(Math.max(newValue, MIN_VALUE));

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
          label={t("supportedApplications")}
          labelIcon={
            <HelpItem
              helpText={t("supportedApplicationsHelp")}
              fieldLabelId="supportedApplications"
            />
          }
        >
          <ChipGroup data-testid="supportedApplications">
            {supportedApplications.map((label) => (
              <Chip key={label} isReadOnly>
                {label}
              </Chip>
            ))}
          </ChipGroup>
        </FormGroup>

        {otpType === POLICY_TYPES[0] && (
          <FormGroup
            label={t("otpPolicyCodeReusable")}
            fieldId="otpPolicyCodeReusable"
            labelIcon={
              <HelpItem
                helpText={t("otpPolicyCodeReusableHelp")}
                fieldLabelId="otpPolicyCodeReusable"
              />
            }
          >
            <Controller
              name="otpPolicyCodeReusable"
              defaultValue={true}
              control={control}
              render={({ field }) => (
                <Switch
                  id="otpPolicyCodeReusable"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                />
              )}
            />
          </FormGroup>
        )}

        <ActionGroup>
          <Button
            data-testid="save"
            variant="primary"
            type="submit"
            isDisabled={!isValid || !isDirty}
          >
            {t("save")}
          </Button>
          <Button
            data-testid="reload"
            variant={ButtonVariant.link}
            onClick={() => reset({ ...realm })}
          >
            {t("reload")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
