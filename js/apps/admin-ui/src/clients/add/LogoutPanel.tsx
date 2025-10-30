import { FormGroup, Switch } from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem, TextControl } from "@keycloak/keycloak-ui-shared";

import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FixedButtonsGroup } from "../../components/form/FixedButtonGroup";
import { FormAccess } from "../../components/form/FormAccess";
import { useAccess } from "../../context/access/Access";
import { convertAttributeNameToForm } from "../../util";
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
  const { control, watch } = useFormContext<FormFields>();

  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-clients") || access?.configure;

  const protocol = watch("protocol");
  const frontchannelLogout = watch("frontchannelLogout");
  const frontchannelLogoutTooltip =
    protocol === "openid-connect"
      ? "frontchannelLogoutOIDCHelp"
      : "frontchannelLogoutHelp";

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
            helpText={t(frontchannelLogoutTooltip)}
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
        <TextControl
          data-testid="frontchannelLogoutUrl"
          type="url"
          name={convertAttributeNameToForm<FormFields>(
            "attributes.frontchannel.logout.url",
          )}
          label={t("frontchannelLogoutUrl")}
          labelIcon={t("frontchannelLogoutUrlHelp")}
          rules={{
            validate: (uri) => validateUrl(uri, t("frontchannelUrlInvalid")),
          }}
        />
      )}
      {protocol === "openid-connect" && frontchannelLogout && (
        <DefaultSwitchControl
          name={convertAttributeNameToForm<FormFields>(
            "attributes.frontchannel.logout.session.required",
          )}
          defaultValue="true"
          label={t("frontchannelLogoutSessionRequired")}
          labelIcon={t("frontchannelLogoutSessionRequiredHelp")}
          stringify
        />
      )}
      {protocol === "openid-connect" && !frontchannelLogout && (
        <>
          <TextControl
            data-testid="backchannelLogoutUrl"
            type="url"
            name={convertAttributeNameToForm<FormFields>(
              "attributes.backchannel.logout.url",
            )}
            label={t("backchannelLogoutUrl")}
            labelIcon={t("backchannelLogoutUrlHelp")}
            rules={{
              validate: (uri) => validateUrl(uri, t("backchannelUrlInvalid")),
            }}
          />
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
              defaultValue="false"
              control={control}
              render={({ field }) => (
                <Switch
                  id="backchannelLogoutSessionRequired"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value === "true"}
                  onChange={(_event, value) => field.onChange(value.toString())}
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
                  onChange={(_event, value) => field.onChange(value.toString())}
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
        isDisabled={!isManager}
      />
    </FormAccess>
  );
};
