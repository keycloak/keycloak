import { Form, FormGroup, Select, SelectOption } from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import React from "react";
import { HelpItem } from "../components/help-enabler/HelpItem";

export const KerberosSettingsCache = () => {
  const { t } = useTranslation("user-federation");
  const helpText = useTranslation("user-federation-help").t;

  return (
    <>
      {/* Cache settings */}
      <Form isHorizontal>
        <FormGroup
          label={t("cachePolicy")}
          labelIcon={
            <HelpItem
              helpText={helpText("cachePolicyHelp")}
              forLabel={t("cachePolicy")}
              forID="kc-cache-policy"
            />
          }
          fieldId="kc-cache-policy"
        >
          <Select
            toggleId="kc-cache-policy"
            // isOpen={openType}
            onToggle={() => {}}
            // variant={SelectVariant.single}
            // value={selected}
            // selections={selected}
            // onSelect={(_, value) => {
            //   setSelected(value as string);
            //   setOpenType(false);
            // }}
            aria-label="Select Input"
          >
            {/* {configFormats.map((configFormat) => ( */}
            <SelectOption
              key={"key"}
              value={"value"}
              // isSelected={selected === configFormat.id}
            >
              {"display name"}
            </SelectOption>
          </Select>
        </FormGroup>
      </Form>
    </>
  );
};
