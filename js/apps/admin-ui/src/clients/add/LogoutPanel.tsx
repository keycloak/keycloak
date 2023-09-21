import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAccess } from "../../context/access/Access";
import { beerify, convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";
import type { ClientSettingsProps } from "../ClientSettings";

const validateUrl = (uri: string | undefined, error: string) =>
  ((uri?.startsWith("https://") || uri?.startsWith("http://")) &&
    !uri.includes("*")) ||
  uri === "" ||
  error;

export const LogoutPanel = ({
  save,
  reset,
  client: { access },
}: ClientSettingsProps) => {
  const { t } = useTranslation();
  const {
    register,
    control,
    watch,
    formState: { errors },
  } = useFormContext<FormFields>();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || access?.configure;

  const protocol = watch("protocol");
  const frontchannelLogout = watch("frontchannelLogout");

  return (
    <FormAccess
      isHorizontal
      fineGrainedAccess={access?.configure}
      role="manage-clients"
    >
      <FormGroup
        label={t("frontchannelLogout")}
        labelIcon={
          <HelpItem
            helpText={t("frontchannelLogoutHelp")}
            fieldLabelId="frontchannelLogout"
          />
        }
        fieldId="kc-frontchannelLogout"
        hasNoPaddingTop
      >
        <Controller
          name="frontchannelLogout"
          defaultValue={true}
          control={control}
          render={({ field }) => (
            <Switch
              id="kc-frontchannelLogout-switch"
              label={t("on")}
              labelOff={t("off")}
              isChecked={field.value}
              onChange={field.onChange}
              aria-label={t("frontchannelLogout")}
            />
          )}
        />
      </FormGroup>
      {protocol === "openid-connect" && frontchannelLogout && (
        <FormGroup
          label={t("frontchannelLogoutUrl")}
          fieldId="frontchannelLogoutUrl"
          labelIcon={
            <HelpItem
              helpText={t("frontchannelLogoutUrlHelp")}
              fieldLabelId="frontchannelLogoutUrl"
            />
          }
          helperTextInvalid={
            errors.attributes?.[beerify("frontchannel.logout.url")]
              ?.message as string
          }
          validated={
            errors.attributes?.[beerify("frontchannel.logout.url")]?.message
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        >
          <KeycloakTextInput
            id="frontchannelLogoutUrl"
            type="url"
            {...register(
              convertAttributeNameToForm<FormFields>(
                "attributes.frontchannel.logout.url",
              ),
              {
                validate: (uri) =>
                  validateUrl(uri, t("frontchannelUrlInvalid").toString()),
              },
            )}
            validated={
              errors.attributes?.[beerify("frontchannel.logout.url")]?.message
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          />
        </FormGroup>
      )}
      {protocol === "openid-connect" && (
        <>
          <FormGroup
            label={t("backchannelLogoutUrl")}
            fieldId="backchannelLogoutUrl"
            labelIcon={
              <HelpItem
                helpText={t("backchannelLogoutUrlHelp")}
                fieldLabelId="backchannelLogoutUrl"
              />
            }
            helperTextInvalid={
              errors.attributes?.[beerify("backchannel.logout.url")]
                ?.message as string
            }
            validated={
              errors.attributes?.[beerify("backchannel.logout.url")]?.message
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          >
            <KeycloakTextInput
              id="backchannelLogoutUrl"
              type="url"
              {...register(
                convertAttributeNameToForm<FormFields>(
                  "attributes.backchannel.logout.url",
                ),
                {
                  validate: (uri) =>
                    validateUrl(uri, t("backchannelUrlInvalid").toString()),
                },
              )}
              validated={
                errors.attributes?.[beerify("backchannel.logout.url")]?.message
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
            />
          </FormGroup>
          <FormGroup
            label={t("backchannelLogoutSessionRequired")}
            labelIcon={
              <HelpItem
                helpText={t("backchannelLogoutSessionRequiredHelp")}
                fieldLabelId="backchannelLogoutSessionRequired"
              />
            }
            fieldId="backchannelLogoutSessionRequired"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.backchannel.logout.session.required",
              )}
              defaultValue="true"
              control={control}
              render={({ field }) => (
                <Switch
                  id="backchannelLogoutSessionRequired"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange(value.toString())}
                  aria-label={t("backchannelLogoutSessionRequired")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("backchannelLogoutRevokeOfflineSessions")}
            labelIcon={
              <HelpItem
                helpText={t("backchannelLogoutRevokeOfflineSessionsHelp")}
                fieldLabelId="backchannelLogoutRevokeOfflineSessions"
              />
            }
            fieldId="backchannelLogoutRevokeOfflineSessions"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.backchannel.logout.revoke.offline.tokens",
              )}
              defaultValue="false"
              control={control}
              render={({ field }) => (
                <Switch
                  id="backchannelLogoutRevokeOfflineSessions"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange(value.toString())}
                  aria-label={t("backchannelLogoutRevokeOfflineSessions")}
                />
              )}
            />
          </FormGroup>
        </>
      )}
      <FixedButtonsGroup
        name="settings"
        save={save}
        reset={reset}
        isActive={isManager}
      />
    </FormAccess>
  );
};
