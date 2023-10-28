import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  PageSection,
  Popover,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
  Text,
  TextContent,
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { useEffect, useState } from "react";
import {
  Controller,
  FormProvider,
  useForm,
  useFormContext,
} from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, useHelp } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

import "./webauthn-policy.css";

const SIGNATURE_ALGORITHMS = [
  "ES256",
  "ES384",
  "ES512",
  "RS256",
  "RS384",
  "RS512",
  "RS1",
] as const;
const ATTESTATION_PREFERENCE = [
  "not specified",
  "none",
  "indirect",
  "direct",
] as const;

const AUTHENTICATOR_ATTACHMENT = [
  "not specified",
  "platform",
  "cross-platform",
] as const;

const RESIDENT_KEY_OPTIONS = ["not specified", "Yes", "No"] as const;

const USER_VERIFY = [
  "not specified",
  "required",
  "preferred",
  "discouraged",
] as const;

type WeauthnSelectProps = {
  name: string;
  label: string;
  options: readonly string[];
  labelPrefix?: string;
  isMultiSelect?: boolean;
};

const WebauthnSelect = ({
  name,
  label,
  options,
  labelPrefix,
  isMultiSelect = false,
}: WeauthnSelectProps) => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  const [open, toggle] = useState(false);
  return (
    <FormGroup
      label={t(label)}
      labelIcon={
        <HelpItem helpText={t(`${label}Help`)} fieldLabelId={label!} />
      }
      fieldId={name}
    >
      <Controller
        name={name}
        defaultValue={options[0]}
        control={control}
        render={({ field }) => (
          <Select
            toggleId={name}
            onToggle={toggle}
            onSelect={(_, selectedValue) => {
              if (isMultiSelect) {
                const changedValue = field.value.find(
                  (item: string) => item === selectedValue,
                )
                  ? field.value.filter((item: string) => item !== selectedValue)
                  : [...field.value, selectedValue];
                field.onChange(changedValue);
              } else {
                field.onChange(selectedValue.toString());
                toggle(false);
              }
            }}
            selections={
              labelPrefix ? t(`${labelPrefix}.${field.value}`) : field.value
            }
            variant={
              isMultiSelect
                ? SelectVariant.typeaheadMulti
                : SelectVariant.single
            }
            aria-label={t(name)}
            typeAheadAriaLabel={t(name)}
            isOpen={open}
          >
            {options.map((option) => (
              <SelectOption
                selected={option === field.value}
                key={option}
                value={option}
              >
                {labelPrefix ? t(`${labelPrefix}.${option}`) : option}
              </SelectOption>
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};

type WebauthnPolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
  isPasswordLess?: boolean;
};

export const WebauthnPolicy = ({
  realm,
  realmUpdated,
  isPasswordLess = false,
}: WebauthnPolicyProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();
  const { realm: realmName } = useRealm();
  const { enabled } = useHelp();
  const form = useForm({ mode: "onChange" });
  const {
    control,
    register,
    setValue,
    handleSubmit,
    formState: { isDirty, errors },
  } = form;

  const namePrefix = isPasswordLess
    ? "webAuthnPolicyPasswordless"
    : "webAuthnPolicy";

  const setupForm = (realm: RealmRepresentation) =>
    convertToFormValues(realm, setValue);

  useEffect(() => setupForm(realm), []);

  const onSubmit = async (realm: RealmRepresentation) => {
    const submittedRealm = convertFormValuesToObject(realm);
    try {
      await adminClient.realms.update({ realm: realmName }, submittedRealm);
      realmUpdated(submittedRealm);
      setupForm(submittedRealm);
      addAlert(t("webAuthnUpdateSuccess"), AlertVariant.success);
    } catch (error) {
      addError("webAuthnUpdateError", error);
    }
  };

  return (
    <PageSection variant="light">
      {enabled && (
        <Popover bodyContent={t(`${namePrefix}FormHelp`)}>
          <TextContent className="keycloak__section_intro__help">
            <Text>
              <QuestionCircleIcon /> {t("webauthnIntro")}
            </Text>
          </TextContent>
        </Popover>
      )}

      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(onSubmit)}
        className="keycloak__webauthn_policies_authentication__form"
      >
        <FormGroup
          label={t("webAuthnPolicyRpEntityName")}
          fieldId="webAuthnPolicyRpEntityName"
          helperTextInvalid={t("required")}
          validated={errors.webAuthnPolicyRpEntityName ? "error" : "default"}
          isRequired
          labelIcon={
            <HelpItem
              helpText={t("webAuthnPolicyRpEntityNameHelp")}
              fieldLabelId="webAuthnPolicyRpEntityName"
            />
          }
        >
          <KeycloakTextInput
            id="webAuthnPolicyRpEntityName"
            data-testid="webAuthnPolicyRpEntityName"
            validated={errors.webAuthnPolicyRpEntityName ? "error" : "default"}
            {...register(`${namePrefix}RpEntityName`, { required: true })}
          />
        </FormGroup>
        <FormProvider {...form}>
          <WebauthnSelect
            name={`${namePrefix}SignatureAlgorithms`}
            label="webAuthnPolicySignatureAlgorithms"
            options={SIGNATURE_ALGORITHMS}
            isMultiSelect
          />
          <FormGroup
            label={t("webAuthnPolicyRpId")}
            labelIcon={
              <HelpItem
                helpText={t("webAuthnPolicyRpIdHelp")}
                fieldLabelId="webAuthnPolicyRpId"
              />
            }
            fieldId="webAuthnPolicyRpId"
          >
            <KeycloakTextInput
              id="webAuthnPolicyRpId"
              data-testid="webAuthnPolicyRpId"
              {...register(`${namePrefix}RpId`)}
            />
          </FormGroup>
          <WebauthnSelect
            name={`${namePrefix}AttestationConveyancePreference`}
            label="webAuthnPolicyAttestationConveyancePreference"
            options={ATTESTATION_PREFERENCE}
            labelPrefix="attestationPreference"
          />
          <WebauthnSelect
            name={`${namePrefix}AuthenticatorAttachment`}
            label="webAuthnPolicyAuthenticatorAttachment"
            options={AUTHENTICATOR_ATTACHMENT}
            labelPrefix="authenticatorAttachment"
          />
          <WebauthnSelect
            name={`${namePrefix}RequireResidentKey`}
            label="webAuthnPolicyRequireResidentKey"
            options={RESIDENT_KEY_OPTIONS}
            labelPrefix="residentKey"
          />
          <WebauthnSelect
            name={`${namePrefix}UserVerificationRequirement`}
            label="webAuthnPolicyUserVerificationRequirement"
            options={USER_VERIFY}
            labelPrefix="userVerify"
          />
          <FormGroup
            label={t("webAuthnPolicyCreateTimeout")}
            fieldId="webAuthnPolicyCreateTimeout"
            helperTextInvalid={t("webAuthnPolicyCreateTimeoutHint")}
            validated={errors.webAuthnPolicyCreateTimeout ? "error" : "default"}
            labelIcon={
              <HelpItem
                helpText={t("webAuthnPolicyCreateTimeoutHelp")}
                fieldLabelId="webAuthnPolicyCreateTimeout"
              />
            }
          >
            <Controller
              name={`${namePrefix}CreateTimeout`}
              defaultValue={0}
              control={control}
              rules={{ min: 0, max: 31536 }}
              render={({ field }) => (
                <TimeSelector
                  data-testid="webAuthnPolicyCreateTimeout"
                  aria-label={t("webAuthnPolicyCreateTimeout")}
                  value={field.value}
                  onChange={field.onChange}
                  units={["second", "minute", "hour"]}
                  validated={
                    errors.webAuthnPolicyCreateTimeout ? "error" : "default"
                  }
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("webAuthnPolicyAvoidSameAuthenticatorRegister")}
            fieldId="webAuthnPolicyAvoidSameAuthenticatorRegister"
            labelIcon={
              <HelpItem
                helpText={t("webAuthnPolicyAvoidSameAuthenticatorRegisterHelp")}
                fieldLabelId="webAuthnPolicyAvoidSameAuthenticatorRegister"
              />
            }
          >
            <Controller
              name={`${namePrefix}AvoidSameAuthenticatorRegister`}
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  id="webAuthnPolicyAvoidSameAuthenticatorRegister"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value}
                  onChange={field.onChange}
                  aria-label={t("webAuthnPolicyAvoidSameAuthenticatorRegister")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("webAuthnPolicyAcceptableAaguids")}
            fieldId="webAuthnPolicyAcceptableAaguids"
            labelIcon={
              <HelpItem
                helpText={t("webAuthnPolicyAcceptableAaguidsHelp")}
                fieldLabelId="webAuthnPolicyAcceptableAaguids"
              />
            }
          >
            <MultiLineInput
              name={`${namePrefix}AcceptableAaguids`}
              aria-label={t("webAuthnPolicyAcceptableAaguids")}
              addButtonLabel="addAaguids"
            />
          </FormGroup>
          <FormGroup
            label={t("webAuthnPolicyExtraOrigins")}
            fieldId="webAuthnPolicyExtraOrigins"
            labelIcon={
              <HelpItem
                helpText={t("webAuthnPolicyExtraOriginsHelp")}
                fieldLabelId="webAuthnPolicyExtraOrigins"
              />
            }
          >
            <MultiLineInput
              name={`${namePrefix}ExtraOrigins`}
              aria-label={t("webAuthnPolicyExtraOrigins")}
              addButtonLabel="addOrigins"
            />
          </FormGroup>
        </FormProvider>

        <ActionGroup>
          <Button
            data-testid="save"
            variant="primary"
            type="submit"
            isDisabled={!isDirty}
          >
            {t("save")}
          </Button>
          <Button
            data-testid="reload"
            variant={ButtonVariant.link}
            onClick={() => setupForm(realm)}
          >
            {t("reload")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
