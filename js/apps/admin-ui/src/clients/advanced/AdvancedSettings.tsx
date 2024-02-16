import RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { FormAccess } from "../../components/form/FormAccess";
import { HelpItem } from "ui-shared";

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

          <TokenLifespan
            id="clientOfflineSessionMax"
            name={convertAttributeNameToForm(
              "attributes.client.offline.session.max.lifespan",
            )}
            defaultValue={
              realm?.offlineSessionMaxLifespanEnabled
                ? realm.offlineSessionMaxLifespan
                : undefined
            }
            units={["minute", "day", "hour"]}
          />

          <FormGroup
            label={t("oAuthMutual")}
            fieldId="oAuthMutual"
            hasNoPaddingTop
            labelIcon={
              <HelpItem
                helpText={t("oAuthMutualHelp")}
                fieldLabelId="oAuthMutual"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.tls.client.certificate.bound.access.tokens",
              )}
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  id="oAuthMutual-switch"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange("" + value)}
                  aria-label={t("oAuthMutual")}
                />
              )}
            />
          </FormGroup>
          {isDPoPEnabled && (
            <FormGroup
              label={t("oAuthDPoP")}
              fieldId="oAuthDPoP"
              hasNoPaddingTop
              labelIcon={
                <HelpItem
                  helpText={t("oAuthDPoPHelp")}
                  fieldLabelId="oAuthDPoP"
                />
              }
            >
              <Controller
                name={convertAttributeNameToForm<FormFields>(
                  "attributes.dpop.bound.access.tokens",
                )}
                defaultValue={false}
                control={control}
                render={({ field }) => (
                  <Switch
                    id="oAuthDPoP-switch"
                    label={t("on")}
                    labelOff={t("off")}
                    isChecked={field.value === "true"}
                    onChange={(value) => field.onChange("" + value)}
                    aria-label={t("oAuthDPoP")}
                  />
                )}
              />
            </FormGroup>
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
                  onToggle={setOpen}
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
          <FormGroup
            label={t("pushedAuthorizationRequestRequired")}
            fieldId="pushedAuthorizationRequestRequired"
            labelIcon={
              <HelpItem
                helpText={t("pushedAuthorizationRequestRequiredHelp")}
                fieldLabelId="pushedAuthorizationRequestRequired"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.require.pushed.authorization.requests",
              )}
              defaultValue="false"
              control={control}
              render={({ field }) => (
                <Switch
                  id="pushedAuthorizationRequestRequired"
                  label={t("on")}
                  labelOff={t("off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange(value.toString())}
                  aria-label={t("pushedAuthorizationRequestRequired")}
                />
              )}
            />
          </FormGroup>
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
