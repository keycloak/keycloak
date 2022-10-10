import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Control, Controller } from "react-hook-form";
import {
  ActionGroup,
  Button,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Switch,
} from "@patternfly/react-core";

import { FormAccess } from "../../components/form-access/FormAccess";
import { HelpItem } from "../../components/help-enabler/HelpItem";
import { TimeSelector } from "../../components/time-selector/TimeSelector";
import { TokenLifespan } from "./TokenLifespan";
import { KeyValueInput } from "../../components/key-value-form/KeyValueInput";
import { MultiLineInput } from "../../components/multi-line-input/MultiLineInput";
import { convertAttributeNameToForm } from "../../util";

type AdvancedSettingsProps = {
  control: Control<Record<string, any>>;
  save: () => void;
  reset: () => void;
  protocol?: string;
  hasConfigureAccess?: boolean;
};

export const AdvancedSettings = ({
  control,
  save,
  reset,
  protocol,
  hasConfigureAccess,
}: AdvancedSettingsProps) => {
  const { t } = useTranslation("clients");
  const [open, setOpen] = useState(false);
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
              helpText="clients-help:assertionLifespan"
              fieldLabelId="clients:assertionLifespan"
            />
          }
        >
          <Controller
            name={convertAttributeNameToForm(
              "attributes.saml.assertion.lifespan"
            )}
            defaultValue=""
            control={control}
            render={({ onChange, value }) => (
              <TimeSelector
                units={["minute", "day", "hour"]}
                value={value}
                onChange={onChange}
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
            defaultValue=""
            units={["minute", "day", "hour"]}
            control={control}
          />

          <FormGroup
            label={t("oAuthMutual")}
            fieldId="oAuthMutual"
            hasNoPaddingTop
            labelIcon={
              <HelpItem
                helpText="clients-help:oAuthMutual"
                fieldLabelId="clients:oAuthMutual"
              />
            }
          >
            <Controller
              name="attributes.tls-client-certificate-bound-access-tokens"
              defaultValue={false}
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  id="oAuthMutual-switch"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange("" + value)}
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
                helpText="clients-help:keyForCodeExchange"
                fieldLabelId="clients:keyForCodeExchange"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm(
                "attributes.pkce.code.challenge.method"
              )}
              defaultValue=""
              control={control}
              render={({ onChange, value }) => (
                <Select
                  toggleId="keyForCodeExchange"
                  variant={SelectVariant.single}
                  onToggle={setOpen}
                  isOpen={open}
                  onSelect={(_, value) => {
                    onChange(value);
                    setOpen(false);
                  }}
                  selections={[value || t("common:choose")]}
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
                helpText="clients-help:pushedAuthorizationRequestRequired"
                fieldLabelId="clients:pushedAuthorizationRequestRequired"
              />
            }
          >
            <Controller
              name={convertAttributeNameToForm(
                "attributes.require.pushed.authorization.requests"
              )}
              defaultValue="false"
              control={control}
              render={({ onChange, value }) => (
                <Switch
                  id="pushedAuthorizationRequestRequired"
                  label={t("common:on")}
                  labelOff={t("common:off")}
                  isChecked={value === "true"}
                  onChange={(value) => onChange(value.toString())}
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
                helpText="clients-help:acrToLoAMapping"
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
                helpText="clients-help:defaultACRValues"
                fieldLabelId="clients:defaultACRValues"
              />
            }
          >
            <MultiLineInput
              name={convertAttributeNameToForm("attributes.default.acr.values")}
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
