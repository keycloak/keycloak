import {
  HelpItem,
  KeycloakSelect,
  SelectVariant,
} from "@keycloak/keycloak-ui-shared";
import {
  ExpandableSection,
  Form,
  FormGroup,
  NumberInput,
  SelectOption,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormGroupField } from "../component/FormGroupField";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

const promptOptions = {
  unspecified: "",
  none: "none",
  consent: "consent",
  login: "login",
  select_account: "select_account",
};

export const ExtendedNonDiscoverySettings = () => {
  const { t } = useTranslation();
  const { control } = useFormContext();

  const [isExpanded, setIsExpanded] = useState(false);
  const [promptOpen, setPromptOpen] = useState(false);

  return (
    <ExpandableSection
      toggleText={t("advanced")}
      onToggle={() => setIsExpanded(!isExpanded)}
      isExpanded={isExpanded}
    >
      <Form isHorizontal>
        <SwitchField label="passLoginHint" field="config.loginHint" />
        <SwitchField label="passMaxAge" field="config.passMaxAge" />
        <SwitchField label="passCurrentLocale" field="config.uiLocales" />
        <SwitchField
          field="config.backchannelSupported"
          label="backchannelLogout"
        />
        <SwitchField
          field="config.sendIdTokenOnLogout"
          label="sendIdTokenOnLogout"
          defaultValue={"true"}
        />
        <SwitchField
          field="config.sendClientIdOnLogout"
          label="sendClientIdOnLogout"
        />
        <SwitchField field="config.disableUserInfo" label="disableUserInfo" />
        <SwitchField field="config.disableNonce" label="disableNonce" />
        <SwitchField
          field="config.disableTypeClaimCheck"
          label="disableTypeClaimCheck"
        />
        <TextField field="config.defaultScope" label="scopes" />
        <FormGroupField label="prompt">
          <Controller
            name="config.prompt"
            defaultValue=""
            control={control}
            render={({ field }) => (
              <KeycloakSelect
                toggleId="prompt"
                onToggle={() => setPromptOpen(!promptOpen)}
                onSelect={(value) => {
                  field.onChange(value as string);
                  setPromptOpen(false);
                }}
                selections={field.value || t(`prompts.unspecified`)}
                variant={SelectVariant.single}
                aria-label={t("prompt")}
                isOpen={promptOpen}
              >
                {Object.entries(promptOptions).map(([key, val]) => (
                  <SelectOption
                    selected={val === field.value}
                    key={key}
                    value={val}
                  >
                    {t(`prompts.${key}`)}
                  </SelectOption>
                ))}
              </KeycloakSelect>
            )}
          />
        </FormGroupField>
        <SwitchField
          field="config.acceptsPromptNoneForwardFromClient"
          label="acceptsPromptNone"
        />
        <SwitchField
          field="config.requiresShortStateParameter"
          label="requiresShortStateParameter"
        />
        <FormGroup
          label={t("allowedClockSkew")}
          labelIcon={
            <HelpItem
              helpText={t("allowedClockSkewHelp")}
              fieldLabelId="allowedClockSkew"
            />
          }
          fieldId="allowedClockSkew"
        >
          <Controller
            name="config.allowedClockSkew"
            defaultValue={0}
            control={control}
            render={({ field }) => {
              const v = Number(field.value);
              return (
                <NumberInput
                  data-testid="allowedClockSkew"
                  inputName="allowedClockSkew"
                  min={0}
                  max={2147483}
                  value={v}
                  readOnly
                  onPlus={() => field.onChange(v + 1)}
                  onMinus={() => field.onChange(v - 1)}
                  onChange={(event) => {
                    const value = Number(
                      (event.target as HTMLInputElement).value,
                    );
                    field.onChange(value < 0 ? 0 : value);
                  }}
                />
              );
            }}
          />
        </FormGroup>
        <TextField field="config.forwardParameters" label="forwardParameters" />
      </Form>
    </ExpandableSection>
  );
};
