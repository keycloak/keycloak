import {
  HelpItem,
  TextControl,
  SelectControl,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  FormGroup,
  TextInput,
} from "@patternfly/react-core";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FormAccess } from "../../components/form/FormAccess";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { useRealm } from "../../context/realm-context/RealmContext";
import { convertAttributeNameToForm } from "../../util";
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

  const { realmRepresentation: realm } = useRealm();

  const { control, watch, register } = useFormContext();

  const acrUriMapRealm = realm?.attributes?.["acr.uri.map"]
    ? Object.values(JSON.parse(realm.attributes["acr.uri.map"]))
    : [];

  const acrLoAMapClient = watch(
    convertAttributeNameToForm<FormFields>("attributes.acr.loa.map"),
    [],
  );

  const validAcrLoAOptions = () =>
    acrLoAMapClient.length > 0
      ? acrLoAMapClient.map((i: any) => i?.key).filter((i: any) => i !== "")
      : acrUriMapRealm;

  const acrLoAMapNamesOptions = () => [
    { key: "", value: t("choose") },
    ...validAcrLoAOptions().map((i: any) => ({ key: i, value: i })),
  ];

  const isFeatureEnabled = useIsFeatureEnabled();

  return (
    <FormAccess
      role="manage-realm"
      fineGrainedAccess={hasConfigureAccess}
      isHorizontal
    >
      {protocol === "saml" && (
        <>
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
          {isFeatureEnabled(Feature.StepUpAuthenticationSaml) && (
            <>
              <FormGroup
                label={t("acrToLoAMapping")}
                fieldId="acrToLoAMapping"
                labelIcon={
                  <HelpItem
                    helpText={t("acrToLoAMappingSamlHelp")}
                    fieldLabelId="acrToLoAMapping"
                  />
                }
              >
                <KeyValueInput
                  label={t("acrToLoAMapping")}
                  name={convertAttributeNameToForm("attributes.acr.loa.map")}
                  keyLabel="uri"
                  valueLabel="loa"
                  ValueComponent={(props) => (
                    <TextInput
                      placeholder={t("loaPlaceholder")}
                      aria-label={t("loa")}
                      validated={props.error ? "error" : "default"}
                      {...register(props.name, {
                        required: true,
                        validate: (v: string) => Number.isInteger(parseInt(v)),
                      })}
                    />
                  )}
                />
              </FormGroup>
              <SelectControl
                name={convertAttributeNameToForm(
                  "attributes.minimum.acr.value",
                )}
                label={t("minimumACRValue")}
                labelIcon={t("minimumACRValueSamlHelp")}
                controller={{
                  defaultValue: "",
                  rules: {
                    validate: (v: string) =>
                      v === "" || validAcrLoAOptions().includes(v),
                  },
                }}
                options={acrLoAMapNamesOptions()}
              />
            </>
          )}
        </>
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

          {realm?.offlineSessionMaxLifespanEnabled && (
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
          )}
          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.tls.client.certificate.bound.access.tokens",
            )}
            label={t("oAuthMutual")}
            labelIcon={t("oAuthMutualHelp")}
            stringify
          />
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

          <DefaultSwitchControl
            name={convertAttributeNameToForm<FormFields>(
              "attributes.client.introspection.response.allow.jwt.claim.enabled",
            )}
            label={t("supportJwtClaimInIntrospectionResponse")}
            labelIcon={t("supportJwtClaimInIntrospectionResponseHelp")}
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
              keyLabel="acr"
              valueLabel="loa"
              ValueComponent={(props) => (
                <TextInput
                  placeholder={t("loaPlaceholder")}
                  aria-label={t("loa")}
                  validated={props.error ? "error" : "default"}
                  {...register(props.name, {
                    required: true,
                    validate: (v: string) => Number.isInteger(parseInt(v)),
                  })}
                />
              )}
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
          <TextControl
            type="text"
            name={convertAttributeNameToForm("attributes.minimum.acr.value")}
            label={t("minimumACRValue")}
            labelIcon={t("minimumACRValueHelp")}
          />
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
