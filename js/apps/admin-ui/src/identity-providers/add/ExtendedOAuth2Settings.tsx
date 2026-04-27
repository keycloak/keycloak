import { NumberControl, SelectControl } from "@keycloak/keycloak-ui-shared";
import { ExpandableSection, Form } from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { SwitchField } from "../component/SwitchField";
import { TextField } from "../component/TextField";

export const ExtendedOAuth2Settings = () => {
  const { t } = useTranslation();

  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <ExpandableSection
      toggleText={t("advanced")}
      onToggle={() => setIsExpanded(!isExpanded)}
      isExpanded={isExpanded}
    >
      <Form isHorizontal>
        <TextField field="config.defaultScope" label="scopes" />
        <SelectControl
          name="config.prompt"
          label={t("prompt")}
          options={[
            { key: "", value: t("prompts.unspecified") },
            { key: "none", value: t("prompts.none") },
            { key: "consent", value: t("prompts.consent") },
            { key: "login", value: t("prompts.login") },
            { key: "select_account", value: t("prompts.select_account") },
          ]}
          controller={{ defaultValue: "" }}
        />
        <SwitchField
          field="config.acceptsPromptNoneForwardFromClient"
          label="acceptsPromptNone"
        />
        <SwitchField
          field="config.requiresShortStateParameter"
          label="requiresShortStateParameter"
        />
        <NumberControl
          name="config.allowedClockSkew"
          label={t("allowedClockSkew")}
          labelIcon={t("allowedClockSkewHelp")}
          controller={{ defaultValue: 0, rules: { min: 0 } }}
        />
        <TextField field="config.forwardParameters" label="forwardParameters" />
      </Form>
    </ExpandableSection>
  );
};
