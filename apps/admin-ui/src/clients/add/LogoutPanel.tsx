import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import { FormGroup, Switch, ValidatedOptions } from "@patternfly/react-core";

import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientSettingsProps } from "../ClientSettings";
import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { useAccess } from "../../context/access/Access";
import { SaveReset } from "../advanced/SaveReset";
import { convertAttributeNameToForm } from "../../util";

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
  } = useFormContext<ClientRepresentation>();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || access?.configure;

  const protocol = watch("protocol");
  const frontchannelLogout = watch("frontchannelLogout");

  return (
    <FormAccess
      isHorizontal
      fineGrainedAccess={access?.configure}
      role="manage-clients"
      className="pf-u-pb-4xl"
    >
      <FormGroup
        label={t("frontchannelLogout")}
        labelIcon={
          <HelpItem
            helpText="clients-help:frontchannelLogout"
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
          render={({ onChange, value }) => (
            <Switch
              id="kc-frontchannelLogout-switch"
              label={t("common:on")}
              labelOff={t("common:off")}
              isChecked={value}
              onChange={onChange}
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
              helpText="clients-help:frontchannelLogoutUrl"
              fieldLabelId="clients:frontchannelLogoutUrl"
            />
          }
          helperTextInvalid={
            errors.attributes?.frontchannel?.logout?.url?.message
          }
          validated={
            errors.attributes?.frontchannel?.logout?.url?.message
              ? ValidatedOptions.error
              : ValidatedOptions.default
          }
        >
          <KeycloakTextInput
            type="text"
            id="frontchannelLogoutUrl"
            name={convertAttributeNameToForm(
              "attributes.frontchannel.logout.url"
            )}
            ref={register({
              validate: (uri) =>
                ((uri.startsWith("https://") || uri.startsWith("http://")) &&
                  !uri.includes("*")) ||
                uri === "" ||
                t("frontchannelUrlInvalid").toString(),
            })}
            validated={
              errors.attributes?.frontchannel?.logout?.url?.message
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
                helpText="clients-help:backchannelLogoutUrl"
                fieldLabelId="clients:backchannelLogoutUrl"
              />
            }
            helperTextInvalid={
              errors.attributes?.backchannel?.logout?.url?.message
            }
            validated={
              errors.attributes?.backchannel?.logout?.url?.message
                ? ValidatedOptions.error
                : ValidatedOptions.default
            }
          >
            <KeycloakTextInput
              type="text"
              id="backchannelLogoutUrl"
              name={convertAttributeNameToForm(
                "attributes.backchannel.logout.url"
              )}
              ref={register({
                validate: (uri) =>
                  ((uri.startsWith("https://") || uri.startsWith("http://")) &&
                    !uri.includes("*")) ||
                  uri === "" ||
                  t("backchannelUrlInvalid").toString(),
              })}
              validated={
                errors.attributes?.backchannel?.logout?.url?.message
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
            />
          </FormGroup>
          <FormGroup
            label={t("backchannelLogoutSessionRequired")}
            labelIcon={
              <HelpItem
                helpText="clients-help:backchannelLogoutSessionRequired"
                fieldLabelId="clients:backchannelLogoutSessionRequired"
              />
            }
            fieldId="backchannelLogoutSessionRequired"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm(
                "attributes.backchannel.logout.session.required"
              )}
              defaultValue="true"
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  id="backchannelLogoutSessionRequired"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange(value.toString())}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("backchannelLogoutRevokeOfflineSessions")}
            labelIcon={
              <HelpItem
                helpText="clients-help:backchannelLogoutRevokeOfflineSessions"
                fieldLabelId="clients:backchannelLogoutRevokeOfflineSessions"
              />
            }
            fieldId="backchannelLogoutRevokeOfflineSessions"
            hasNoPaddingTop
          >
            <Controller
              name={convertAttributeNameToForm(
                "attributes.backchannel.logout.revoke.offline.tokens"
              )}
              defaultValue="false"
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  id="backchannelLogoutRevokeOfflineSessions"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange(value.toString())}
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
