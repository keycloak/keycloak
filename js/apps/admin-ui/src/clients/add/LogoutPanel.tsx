import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAccess } from "../../context/access/Access";
import { beerify, convertAttributeNameToForm } from "../../util";
import { SaveReset } from "../advanced/SaveReset";
import type { ClientSettingsProps } from "../ClientSettings";
import { FormFields } from "../ClientDetails";

export const LogoutPanel = ({
  save,
  reset,
  client: { access },
}: ClientSettingsProps) => {
  const { t } = useTranslation("clients");
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
            helpText={t("clients-help:frontchannelLogout")}
            fieldLabelId="clients:frontchannelLogout"
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
              label={t("common:on")}
              labelOff={t("common:off")}
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
              helpText={t("clients-help:frontchannelLogoutUrl")}
              fieldLabelId="clients:frontchannelLogoutUrl"
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
                "attributes.frontchannel.logout.url"
              ),
              {
                validate: (uri) =>
                  ((uri.startsWith("https://") || uri.startsWith("http://")) &&
                    !uri.includes("*")) ||
                  uri === "" ||
                  t("frontchannelUrlInvalid").toString(),
              }
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
                helpText={t("clients-help:backchannelLogoutUrl")}
                fieldLabelId="clients:backchannelLogoutUrl"
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
                  "attributes.backchannel.logout.url"
                ),
                {
                  validate: (uri) =>
                    ((uri.startsWith("https://") ||
                      uri.startsWith("http://")) &&
                      !uri.includes("*")) ||
                    uri === "" ||
                    t("backchannelUrlInvalid").toString(),
                }
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
                helpText={t("clients-help:backchannelLogoutSessionRequired")}
                fieldLabelId="clients:backchannelLogoutSessionRequired"
              />
            }
            fieldId="backchannelLogoutSessionRequired"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.backchannel.logout.session.required"
              )}
              defaultValue="true"
              control={control}
              render={({ field }) => (
                <Switch
                  id="backchannelLogoutSessionRequired"
                  label={t("common:on")}
                  labelOff={t("common:off")}
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
                helpText={t(
                  "clients-help:backchannelLogoutRevokeOfflineSessions"
                )}
                fieldLabelId="clients:backchannelLogoutRevokeOfflineSessions"
              />
            }
            fieldId="backchannelLogoutRevokeOfflineSessions"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.backchannel.logout.revoke.offline.tokens"
              )}
              defaultValue="false"
              control={control}
              render={({ field }) => (
                <Switch
                  id="backchannelLogoutRevokeOfflineSessions"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange(value.toString())}
                  aria-label={t("backchannelLogoutRevokeOfflineSessions")}
                />
              )}
            />
          </FormGroup>
        </>
      )}
      <SaveReset
        className="keycloak__form_actions"
        name="settings"
        save={save}
        reset={reset}
        isActive={isManager}
      />
    </FormAccess>
  );
};
