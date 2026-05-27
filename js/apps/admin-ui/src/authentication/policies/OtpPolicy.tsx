import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import type OtpPolicyRepresentation from "@keycloak/keycloak-admin-client/lib/defs/otpPolicyRepresentation"
import {
  HelpItem,
  NumberControl,
  SelectControl,
  SwitchControl,
  useAlerts,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  Chip,
  ChipGroup,
  FormGroup,
  PageSection,
  Radio,
} from "@patternfly/react-core";
import { useMemo, useState } from "react";
import { Controller, FormProvider, useForm, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import { FormAccess } from "../../components/form/FormAccess";
import { TimeSelectorControl } from "../../components/time-selector/TimeSelectorControl";
import { useRealm } from "../../context/realm-context/RealmContext";
import useLocaleSort from "../../utils/useLocaleSort";

import "./otp-policy.css";

const POLICY_TYPES = ["totp", "hotp"] as const;
const OTP_HASH_ALGORITHMS = ["SHA1", "SHA256", "SHA512"] as const;
const NUMBER_OF_DIGITS = [6, 8] as const;

type OtpPolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
};

export const OtpPolicy = ({ realm, realmUpdated }: OtpPolicyProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const form = useForm<OtpPolicyRepresentation>({
    mode: "onChange",
    defaultValues: {
      type: POLICY_TYPES[0],
      algorithm: `Hmac${OTP_HASH_ALGORITHMS[0]}`,
      digits: NUMBER_OF_DIGITS[0],
      lookAheadWindow: 1,
      period: 30,
      initialCounter: 30,
      codeReusable: false,
    },
  });
  const {
    control,
    reset,
    handleSubmit,
    formState: { isValid, isDirty },
  } = form;
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();
  const localeSort = useLocaleSort();
  
  const [key, setKey] = useState(0);
  const refresh = () => setKey(new Date().getTime());

  const otpType = useWatch({ name: "type", control });

  const setupForm = (formValues: OtpPolicyRepresentation) => reset(formValues);

  const supportedApplications = useMemo(() => {
    const labels = (realm.otpSupportedApplications ?? []).map((key) =>
      t(`otpSupportedApplications.${key}`),
    );

    return localeSort(labels, (label) => label);
  }, [realm.otpSupportedApplications]);

  useFetch(
    () => adminClient.authenticationManagement.getOtpPolicy({ realm: realmName }),
    (otpPolicy) => {
      setupForm(otpPolicy);
    },
    [key],
  );

  const onSubmit = async (formValues: OtpPolicyRepresentation) => {
    try {
      await adminClient.authenticationManagement.updateOtpPolicy({ realm: realmName }, formValues);
      const updatedRealm = await adminClient.realms.findOne({
        realm: realmName,
      });
      realmUpdated(updatedRealm!);
      refresh();
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
        <FormProvider {...form}>
          <FormGroup
            label={t("otpType")}
            labelIcon={
              <HelpItem helpText={t("otpTypeHelp")} fieldLabelId="otpType" />
            }
            hasNoPaddingTop
          >
            <Controller
              name="type"
              data-testid="otpPolicyType"
              defaultValue={POLICY_TYPES[0]}
              control={control}
              render={({ field: { value, onChange } }) => (
                <>
                  {POLICY_TYPES.map((type) => (
                    <Radio
                      key={type}
                      id={type}
                      data-testid={type}
                      isChecked={value === type}
                      name="type"
                      onChange={() => onChange(type)}
                      label={t(`policyType.${type}`)}
                      className="keycloak__otp_policies_authentication__policy-type"
                    />
                  ))}
                </>
              )}
            />
          </FormGroup>
          <SelectControl
            name="algorithm"
            label={t("otpHashAlgorithm")}
            labelIcon={t("otpHashAlgorithmHelp")}
            options={OTP_HASH_ALGORITHMS.map((type) => ({
              key: `Hmac${type}`,
              value: type,
            }))}
            controller={{ defaultValue: `Hmac${OTP_HASH_ALGORITHMS[0]}` }}
          />
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
              name="digits"
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
                      name="digits"
                      onChange={() => field.onChange(type)}
                      label={type}
                      className="keycloak__otp_policies_authentication__number-of-digits"
                    />
                  ))}
                </>
              )}
            />
          </FormGroup>
          <NumberControl
            name="lookAheadWindow"
            label={t("lookAround")}
            labelIcon={t("lookAroundHelp")}
            controller={{ defaultValue: 1, rules: { min: 0 } }}
          />
          {otpType === POLICY_TYPES[0] && (
            <TimeSelectorControl
              name="period"
              label={t("otpPolicyPeriod")}
              labelIcon={t("otpPolicyPeriodHelp")}
              units={["second", "minute"]}
              controller={{
                defaultValue: 30,
                rules: {
                  min: 1,
                  max: {
                    value: 120,
                    message: t("maxLength", { length: "2 " + t("minutes") }),
                  },
                },
              }}
            />
          )}
          {otpType === POLICY_TYPES[1] && (
            <NumberControl
              name="initialCounter"
              label={t("initialCounter")}
              labelIcon={t("initialCounterHelp")}
              controller={{ defaultValue: 30, rules: { min: 1, max: 120 } }}
            />
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
            <span data-testid="supportedApplications">
              <ChipGroup>
                {supportedApplications.map((label) => (
                  <Chip key={label} isReadOnly>
                    {label}
                  </Chip>
                ))}
              </ChipGroup>
            </span>
          </FormGroup>

          {otpType === POLICY_TYPES[0] && (
            <SwitchControl
              name="codeReusable"
              label={t("otpPolicyCodeReusable")}
              labelIcon={t("otpPolicyCodeReusableHelp")}
              labelOn={t("on")}
              labelOff={t("off")}
            />
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
              onClick={() => refresh()}
            >
              {t("reload")}
            </Button>
          </ActionGroup>
        </FormProvider>
      </FormAccess>
    </PageSection>
  );
};
