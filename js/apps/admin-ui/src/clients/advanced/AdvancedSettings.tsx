import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { ActionGroup, Button, FormGroup } from "@patternfly/react-core";
import {
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem } from "ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { adminClient } from "../../admin-client";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertAttributeNameToForm } from "../../util";
import { useFetch } from "../../utils/useFetch";
import { FormFields } from "../ClientDetails";
import { TokenLifespan } from "./TokenLifespan";

import useIsFeatureEnabled, { Feature } from "../../utils/useIsFeatureEnabled";

type AdvancedSettingsProps = {
  save: () => void;
  reset: () => void;
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const AdvancedSettings = ({
  save,
  reset,
  protocol,
  hasConfigureAccess,
}: AdvancedSettingsProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const [realm, setRealm] = useState<RealmRepresentation>();
  const { realm: realmName } = useRealm();

  const isFeatureEnabled = useIsFeatureEnabled();
  const isDPoPEnabled = isFeatureEnabled(Feature.DPoP);

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    setRealm,
    [],
  );

  const { control } = useFormContext();
  return (
    <FormAccess
      role="manage-realm"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      {protocol !== "openid-connect" && (
        <FormGroup
          label={t("assertionLifespan")}
          fieldId="assertionLifespan"
          labelIcon={
            <HelpItem
              helpText={t("assertionLifespanHelp")}
              fieldLabelId="assertionLifespan"
            />
          }
        >
          <Controller
            name={convertAttributeNameToForm<FormFields>(
              "attributes.saml.assertion.lifespan",
            )}
            defaultValue=""
            control={control}
            render={({ field }) => (
              <TimeSelector
                units={["minute", "day", "hour"]}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
        </FormGroup>
      )}
      {protocol === "openid-connect" && (
        <>
          <TokenLifespan
            id="accessTokenLifespan"
            name={convertAttributeNameToForm(
              "attributes.access.token.lifespan",
            )}
            defaultValue={realm?.accessTokenLifespan}
            units={["minute", "day", "hour"]}
          />
          <TokenLifespan
            id="clientSessionIdle"
            name={convertAttributeNameToForm(
              "attributes.client.session.idle.timeout",
            )}
            defaultValue={realm?.clientSessionIdleTimeout}
            units={["minute", "day", "hour"]}
          />
          <TokenLifespan
            id="clientSessionMax"
            name={convertAttributeNameToForm(
              "attributes.client.session.max.lifespan",
            )}
            defaultValue={realm?.clientSessionMaxLifespan}
            units={["minute", "day", "hour"]}
          />
          <TokenLifespan
            id="clientOfflineSessionIdle"
            name={convertAttributeNameToForm(
              "attributes.client.offline.session.idle.timeout",
            )}
            defaultValue={realm?.offlineSessionIdleTimeout}
            units={["minute", "day", "hour"]}
          />
          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.tls.client.certificate.bound.access.tokens",
            )}
            label={t("oAuthMutual")}
            labelIcon={t("oAuthMutualHelp")}
            stringify
          />
          {isDPoPEnabled && (
            <DefaultSwitchControl
              name={convertAttributeNameToForm<FormFields>(
                "attributes.dpop.bound.access.tokens",
              )}
              label={t("oAuthDPoP")}
              labelIcon={t("oAuthDPoPHelp")}
              stringify
            />
          )}
          <FormGroup
            label={t("keyForCodeExchange")}
            fieldId="keyForCodeExchange"
            hasNoPaddingTop
            labelIcon={
              <HelpItem
                helpText={t("keyForCodeExchangeHelp")}
                fieldLabelId="keyForCodeExchange"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.pkce.code.challenge.method",
              )}
              defaultValue=""
              control={control}
              render={({ field }) => (
                <Select
                  toggleId="keyForCodeExchange"
                  variant={SelectVariant.single}
                  onToggle={(_event, val) => setOpen(val)}
                  isOpen={open}
                  onSelect={(_, value) => {
                    field.onChange(value);
                    setOpen(false);
                  }}
                  selections={[field.value || t("choose")]}
                >
                  {["", "S256", "plain"].map((v) => (
                    <SelectOption key={v} value={v}>
                      {v || t("choose")}
                    </SelectOption>
                  ))}
                </Select>
              )}
            />
          </FormGroup>
          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.require.pushed.authorization.requests",
            )}
            label={t("pushedAuthorizationRequestRequired")}
            labelIcon={t("pushedAuthorizationRequestRequiredHelp")}
            stringify
          />
          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.client.use.lightweight.access.token.enabled",
            )}
            label={t("lightweightAccessToken")}
            labelIcon={t("lightweightAccessTokenHelp")}
            stringify
          />
          <FormGroup
            label={t("acrToLoAMapping")}
            fieldId="acrToLoAMapping"
            labelIcon={
              <HelpItem
                helpText={t("acrToLoAMappingHelp")}
                fieldLabelId="acrToLoAMapping"
              />
            }
          >
            <KeyValueInput
              label={t("acrToLoAMapping")}
              name={convertAttributeNameToForm("attributes.acr.loa.map")}
            />
          </FormGroup>
          <FormGroup
            label={t("defaultACRValues")}
            fieldId="defaultACRValues"
            labelIcon={
              <HelpItem
                helpText={t("defaultACRValuesHelp")}
                fieldLabelId="defaultACRValues"
              />
            }
          >
            <MultiLineInput
              id="defaultACRValues"
              aria-label="defaultACRValues"
              name={convertAttributeNameToForm("attributes.default.acr.values")}
              stringify
            />
          </FormGroup>
        </>
      )}
      <ActionGroup>
        <Button
          variant="secondary"
          onClick={save}
          data-testid="OIDCAdvancedSave"
        >
          {t("save")}
        </Button>
        <Button variant="link" onClick={reset} data-testid="OIDCAdvancedRevert">
          {t("revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
