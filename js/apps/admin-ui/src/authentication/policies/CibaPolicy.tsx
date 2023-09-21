import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  ButtonVariant,
  FormGroup,
  InputGroup,
  InputGroupText,
  PageSection,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useAlerts } from "../../components/alert/Alerts";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertFormValuesToObject, convertToFormValues } from "../../util";

const CIBA_BACKHANNEL_TOKEN_DELIVERY_MODES = ["poll", "ping"] as const;
const CIBA_EXPIRES_IN_MIN = 10;
const CIBA_EXPIRES_IN_MAX = 600;
const CIBA_INTERVAL_MIN = 0;
const CIBA_INTERVAL_MAX = 600;

type CibaPolicyProps = {
  realm: RealmRepresentation;
  realmUpdated: (realm: RealmRepresentation) => void;
};

type FormFields = Omit<
  RealmRepresentation,
  "clients" | "components" | "groups"
>;

export const CibaPolicy = ({ realm, realmUpdated }: CibaPolicyProps) => {
  const { t } = useTranslation();
  const {
    control,
    register,
    handleSubmit,
    setValue,
    formState: { errors, isValid, isDirty },
  } = useForm<FormFields>({ mode: "onChange" });
  const { realm: realmName } = useRealm();
  const { addAlert, addError } = useAlerts();
  const [
    backchannelTokenDeliveryModeOpen,
    setBackchannelTokenDeliveryModeOpen,
  ] = useState(false);
  const [authRequestedUserHintOpen, setAuthRequestedUserHintOpen] =
    useState(false);

  const setupForm = (realm: RealmRepresentation) =>
    convertToFormValues(realm, setValue);

  useEffect(() => setupForm(realm), []);

  const onSubmit = async (formValues: FormFields) => {
    try {
      await adminClient.realms.update(
        { realm: realmName },
        convertFormValuesToObject(formValues),
      );

      const updatedRealm = await adminClient.realms.findOne({
        realm: realmName,
      });

      realmUpdated(updatedRealm!);
      setupForm(updatedRealm!);
      addAlert(t("updateCibaSuccess"), AlertVariant.success);
    } catch (error) {
      addError("updateCibaError", error);
    }
  };

  return (
    <PageSection variant="light">
      <FormAccess
        role="manage-realm"
        isHorizontal
        onSubmit={handleSubmit(onSubmit)}
      >
        <FormGroup
          fieldId="cibaBackchannelTokenDeliveryMode"
          label={t("cibaBackchannelTokenDeliveryMode")}
          labelIcon={
            <HelpItem
              helpText={t("cibaBackchannelTokenDeliveryModeHelp")}
              fieldLabelId="cibaBackchannelTokenDeliveryMode"
            />
          }
        >
          <Controller
            name="attributes.cibaBackchannelTokenDeliveryMode"
            defaultValue={CIBA_BACKHANNEL_TOKEN_DELIVERY_MODES[0]}
            control={control}
            render={({ field }) => (
              <Select
                toggleId="cibaBackchannelTokenDeliveryMode"
                onSelect={(_, value) => {
                  setBackchannelTokenDeliveryModeOpen(false);
                  field.onChange(value.toString());
                }}
                selections={field.value}
                variant={SelectVariant.single}
                isOpen={backchannelTokenDeliveryModeOpen}
                onToggle={(isExpanded) =>
                  setBackchannelTokenDeliveryModeOpen(isExpanded)
                }
              >
                {CIBA_BACKHANNEL_TOKEN_DELIVERY_MODES.map((value) => (
                  <SelectOption
                    key={value}
                    value={value}
                    selected={value === field.value}
                  >
                    {t(`cibaBackhannelTokenDeliveryModes.${value}`)}
                  </SelectOption>
                ))}
              </Select>
            )}
          />
        </FormGroup>
        <FormGroup
          fieldId="cibaExpiresIn"
          label={t("cibaExpiresIn")}
          labelIcon={
            <HelpItem
              helpText={t("cibaExpiresInHelp")}
              fieldLabelId="cibaExpiresIn"
            />
          }
          validated={errors.attributes?.cibaExpiresIn ? "error" : "default"}
          helperTextInvalid={
            errors.attributes?.cibaExpiresIn?.message as string
          }
          isRequired
        >
          <InputGroup>
            <KeycloakTextInput
              id="cibaExpiresIn"
              type="number"
              min={CIBA_EXPIRES_IN_MIN}
              max={CIBA_EXPIRES_IN_MAX}
              {...register("attributes.cibaExpiresIn", {
                min: {
                  value: CIBA_EXPIRES_IN_MIN,
                  message: t("greaterThan", {
                    value: CIBA_EXPIRES_IN_MIN,
                  }),
                },
                max: {
                  value: CIBA_EXPIRES_IN_MAX,
                  message: t("lessThan", { value: CIBA_EXPIRES_IN_MAX }),
                },
                required: {
                  value: true,
                  message: t("required"),
                },
              })}
              validated={errors.attributes?.cibaExpiresIn ? "error" : "default"}
            />
            <InputGroupText variant="plain">
              {t("times:seconds")}
            </InputGroupText>
          </InputGroup>
        </FormGroup>
        <FormGroup
          fieldId="cibaInterval"
          label={t("cibaInterval")}
          labelIcon={
            <HelpItem
              helpText={t("cibaIntervalHelp")}
              fieldLabelId="cibaInterval"
            />
          }
          validated={errors.attributes?.cibaInterval ? "error" : "default"}
          helperTextInvalid={errors.attributes?.cibaInterval?.message as string}
          isRequired
        >
          <InputGroup>
            <KeycloakTextInput
              id="cibaInterval"
              type="number"
              min={CIBA_INTERVAL_MIN}
              max={CIBA_INTERVAL_MAX}
              {...register("attributes.cibaInterval", {
                min: {
                  value: CIBA_INTERVAL_MIN,
                  message: t("greaterThan", {
                    value: CIBA_INTERVAL_MIN,
                  }),
                },
                max: {
                  value: CIBA_INTERVAL_MAX,
                  message: t("lessThan", { value: CIBA_INTERVAL_MAX }),
                },
                required: {
                  value: true,
                  message: t("required"),
                },
              })}
              validated={errors.attributes?.cibaInterval ? "error" : "default"}
            />
            <InputGroupText variant="plain">
              {t("times:seconds")}
            </InputGroupText>
          </InputGroup>
        </FormGroup>
        <FormGroup
          fieldId="cibaAuthRequestedUserHint"
          label={t("cibaAuthRequestedUserHint")}
          labelIcon={
            <HelpItem
              helpText={t("cibaAuthRequestedUserHintHelp")}
              fieldLabelId="cibaAuthRequestedUserHint"
            />
          }
        >
          <Select
            toggleId="cibaAuthRequestedUserHint"
            selections="login_hint"
            isOpen={authRequestedUserHintOpen}
            onToggle={(isExpanded) => setAuthRequestedUserHintOpen(isExpanded)}
            isDisabled
          >
            <SelectOption value="login_hint">login_hint</SelectOption>
            <SelectOption value="id_token_hint">id_token_hint</SelectOption>
            <SelectOption value="login_hint_token">
              login_hint_token
            </SelectOption>
          </Select>
        </FormGroup>
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
            onClick={() => setupForm({ ...realm })}
          >
            {t("reload")}
          </Button>
        </ActionGroup>
      </FormAccess>
    </PageSection>
  );
};
