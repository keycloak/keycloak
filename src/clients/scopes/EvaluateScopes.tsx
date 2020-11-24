import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  ClipboardCopy,
  Form,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
  Split,
  SplitItem,
} from "@patternfly/react-core";

import { HelpItem } from "../../components/help-enabler/HelpItem";
import "./evaluate.css";

export const EvaluateScopes = () => {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  // const [selected]

  return (
    <Form isHorizontal>
      <FormGroup
        label={t("rootUrl")}
        fieldId="kc-root-url"
        labelIcon={
          <HelpItem
            helpText="client-scopes-help:protocolMapper"
            forLabel={t("protocolMapper")}
            forID="protocolMapper"
          />
        }
      >
        <Split hasGutter>
          <SplitItem isFilled>
            <Select
              variant={SelectVariant.typeaheadMulti}
              typeAheadAriaLabel="Select a state"
              onToggle={() => setIsOpen(!isOpen)}
              isOpen={isOpen}
              aria-labelledby="test"
              placeholderText="Select a state"
            >
              {/* {this.state.options.map((option, index) => (
            <SelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              {...(option.description && { description: option.description })}
            />
          ))} */}
            </Select>
          </SplitItem>
          <SplitItem>
            <ClipboardCopy className="keycloak__scopes_evaluate__clipboard-copy">
              {isOpen}
            </ClipboardCopy>
          </SplitItem>
        </Split>
      </FormGroup>
    </Form>
  );
};
