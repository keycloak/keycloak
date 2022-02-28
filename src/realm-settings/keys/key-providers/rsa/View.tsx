import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useFormContext, Controller } from "react-hook-form";
import {
  FileUpload,
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
  const [isRSAalgDropdownOpen, toggleDropdown] = useToggle();
  const [privateKey, setPrivateKey] = useState("");
  const [certificate, setCertificate] = useState("");

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const rsaAlgOptions = allComponentTypes[4].properties[3].options ?? [];

  return (
    <>
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
          defaultValue={[rsaAlgOptions[0]]}
          control={control}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-rsa-algorithm"
              onToggle={() => toggleDropdown()}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                toggleDropdown();
              }}
              selections={[value.toString()]}
              variant={SelectVariant.single}
              aria-label={t("algorithm")}
              isOpen={isRSAalgDropdownOpen}
              data-testid="select-rsa-algorithm"
            >
              {rsaAlgOptions!.map((p, idx) => (
                <SelectOption selected={p === value} key={idx} value={p} />
              ))}
            </Select>
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("privateRSAKey")}
        fieldId="kc-private-rsa-key"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:privateRSAKey"
            fieldLabelId="realm-settings:privateRSAKey"
          />
        }
      >
        <Controller
          name="config.privateKey"
          control={control}
          defaultValue={[]}
          render={({ onChange, value }) => (
            <FileUpload
              id="importPrivateKey"
              type="text"
              value={value.value}
              filename={privateKey}
              onChange={(value, filename) => {
                onChange(value);
                setPrivateKey(filename);
              }}
              filenamePlaceholder={t("filenamePlaceholder")}
            />
          )}
        />
      </FormGroup>
      <FormGroup
        label={t("x509Certificate")}
        fieldId="kc-x509Certificate"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:x509Certificate"
            fieldLabelId="realm-settings:x509Certificate"
          />
        }
      >
        <Controller
          name="config.certificate"
          control={control}
          defaultValue={[]}
          render={({ onChange, value }) => (
            <FileUpload
              id="importCertificate"
              type="text"
              value={value}
              filename={certificate}
              onChange={(value, filename) => {
                onChange(value);
                setCertificate(filename);
              }}
              filenamePlaceholder={t("filenamePlaceholder")}
            />
          )}
        />
      </FormGroup>
    </>
  );
}
