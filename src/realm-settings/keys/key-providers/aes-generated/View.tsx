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

  const [isKeySizeDropdownOpen, toggleDropdown] = useToggle();

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];
  const aesSecretSizeOptions = allComponentTypes[0].properties[3].options ?? [];

  return (
    <FormGroup
      label={t("AESKeySize")}
      fieldId="kc-aes-keysize"
      labelIcon={
        <HelpItem
          helpText="realm-settings-help:AESKeySize"
          fieldLabelId="realm-settings:AESKeySize"
        />
      }
    >
      <Controller
        name="config.secretSize"
        control={control}
        defaultValue={[aesSecretSizeOptions[0]]}
        render={({ onChange, value }) => (
          <Select
            toggleId="kc-aes-keysize"
            onToggle={toggleDropdown}
            onSelect={(_, value) => {
              onChange([value.toString()]);
              toggleDropdown();
            }}
            selections={[value.toString()]}
            isOpen={isKeySizeDropdownOpen}
            variant={SelectVariant.single}
            aria-label={t("aesKeySize")}
            data-testid="select-secret-size"
          >
            {aesSecretSizeOptions.map((item) => (
              <SelectOption selected={item === value} key={item} value={item} />
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
}
