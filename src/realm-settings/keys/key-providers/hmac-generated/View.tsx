import React from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, Controller } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import { useServerInfo } from "../../../../context/server-info/ServerInfoProvider";
import { HelpItem } from "../../../../components/help-enabler/HelpItem";
import { KEY_PROVIDER_TYPE } from "../../../../util";
import useToggle from "../../../../utils/useToggle";

export default function View() {
  const { t } = useTranslation("realm-settings");
  const { control } = useFormContext();

  const [isKeySizeDropdownOpen, toggleKeySizeDropdown] = useToggle();
  const [isEllipticCurveDropdownOpen, toggleEllipticDropdown] = useToggle();

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const hmacSecretSizeOptions =
    allComponentTypes[2].properties[3].options ?? [];

  const hmacAlgorithmOptions = allComponentTypes[2].properties[4].options ?? [];

  return (
    <>
      <FormGroup
        label={t("secretSize")}
        fieldId="kc-hmac-keysize"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:secretSize"
            fieldLabelId="realm-settings:secretSize"
          />
        }
      >
        <Controller
          name="config.secretSize"
          control={control}
          defaultValue={[hmacSecretSizeOptions[3]]}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-hmac-keysize"
              onToggle={toggleKeySizeDropdown}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                toggleKeySizeDropdown();
              }}
              selections={[value.toString()]}
              isOpen={isKeySizeDropdownOpen}
              variant={SelectVariant.single}
              aria-label={t("hmacKeySize")}
              data-testid="select-secret-size"
            >
              {hmacSecretSizeOptions.map((item) => (
                <SelectOption
                  selected={item === value}
                  key={item}
                  value={item}
                />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("algorithm")}
        fieldId="kc-algorithm"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:algorithm"
            fieldLabelId="realm-settings:algorithm"
          />
        }
      >
        <Controller
          name="config.algorithm"
          control={control}
          defaultValue={[hmacAlgorithmOptions[0]]}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-elliptic"
              onToggle={toggleEllipticDropdown}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                toggleEllipticDropdown();
              }}
              selections={[value.toString()]}
              variant={SelectVariant.single}
              aria-label={t("emailTheme")}
              isOpen={isEllipticCurveDropdownOpen}
            >
              {hmacAlgorithmOptions!.map((p, idx) => (
                <SelectOption selected={p === value} key={idx} value={p} />
              ))}
            </Select>
          )}
        />
      </FormGroup>
    </>
  );
}
