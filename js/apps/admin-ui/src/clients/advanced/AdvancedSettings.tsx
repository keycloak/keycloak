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

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "ui-shared";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";
import { TokenLifespan } from "./TokenLifespan";

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
  const { t } = useTranslation("clients");
  const [open, setOpen] = useState(false);

  const [realm, setRealm] = useState<RealmRepresentation>();
  const { realm: realmName } = useRealm();
  const { adminClient } = useAdminClient();

  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    setRealm,
    []
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
              helpText={t("clients-help:assertionLifespan")}
              fieldLabelId="clients:assertionLifespan"
            />
          }
        >
          <Controller
            name={convertAttributeNameToForm<FormFields>(
              "attributes.saml.assertion.lifespan"
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
              "attributes.access.token.lifespan"
            )}
            defaultValue={realm?.accessTokenLifespan}
            units={["minute", "day", "hour"]}
          />

          <TokenLifespan
            id="clientSessionIdle"
            name={convertAttributeNameToForm(
              "attributes.client.session.idle.timeout"
            )}
            defaultValue={realm?.clientSessionIdleTimeout}
            units={["minute", "day", "hour"]}
          />

          <TokenLifespan
            id="clientSessionMax"
            name={convertAttributeNameToForm(
              "attributes.client.session.max.lifespan"
            )}
            defaultValue={realm?.clientSessionMaxLifespan}
            units={["minute", "day", "hour"]}
          />

          <TokenLifespan
            id="clientOfflineSessionIdle"
            name={convertAttributeNameToForm(
              "attributes.client.offline.session.idle.timeout"
            )}
            defaultValue={realm?.offlineSessionIdleTimeout}
            units={["minute", "day", "hour"]}
          />

          <TokenLifespan
            id="clientOfflineSessionMax"
            name={convertAttributeNameToForm(
              "attributes.client.offline.session.max.lifespan"
            )}
            defaultValue={realm?.offlineSessionMaxLifespan}
            units={["minute", "day", "hour"]}
          />

          <FormGroup
            label={t("oAuthMutual")}
            fieldId="oAuthMutual"
            hasNoPaddingTop
            labelIcon={
              <HelpItem
                helpText={t("clients-help:oAuthMutual")}
                fieldLabelId="clients:oAuthMutual"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.tls.client.certificate.bound.access.tokens"
              )}
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  id="oAuthMutual-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={field.value === "true"}
                  onChange={(value) => field.onChange("" + value)}
                  aria-label={t("oAuthMutual")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("keyForCodeExchange")}
            fieldId="keyForCodeExchange"
            hasNoPaddingTop
            labelIcon={
              <HelpItem
                helpText={t("clients-help:keyForCodeExchange")}
                fieldLabelId="clients:keyForCodeExchange"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.pkce.code.challenge.method"
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
                  selections={[field.value || t("common:choose")]}
                >
                  {["", "S256", "plain"].map((v) => (
                    <SelectOption key={v} value={v}>
                      {v || t("common:choose")}
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
                helpText={t("clients-help:pushedAuthorizationRequestRequired")}
                fieldLabelId="clients:pushedAuthorizationRequestRequired"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm<FormFields>(
                "attributes.require.pushed.authorization.requests"
              )}
              defaultValue="false"
              control={control}
              render={({ field }) => (
                <Switch
                  id="pushedAuthorizationRequestRequired"
                  label={t("common:on")}
                  labelOff={t("common:off")}
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
                helpText={t("clients-help:acrToLoAMapping")}
                fieldLabelId="clients:acrToLoAMapping"
              />
            }
          >
            <KeyValueInput
              name={convertAttributeNameToForm("attributes.acr.loa.map")}
            />
          </FormGroup>
          <FormGroup
            label={t("defaultACRValues")}
            fieldId="defaultACRValues"
            labelIcon={
              <HelpItem
                helpText={t("clients-help:defaultACRValues")}
                fieldLabelId="clients:defaultACRValues"
              />
            }
          >
            <MultiLineInput
              id="defaultACRValues"
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
          {t("common:save")}
        </Button>
        <Button variant="link" onClick={reset} data-testid="OIDCAdvancedRevert">
          {t("common:revert")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
