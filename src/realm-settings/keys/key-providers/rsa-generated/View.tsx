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

export default function View({ isEnc = false }: { isEnc?: boolean }) {
  const { t } = useTranslation("realm-settings");
  const { control } = useFormContext();

  const [isKeySizeDropdownOpen, toggleKeySizeDropdown] = useToggle();
  const [isEllipticCurveDropdownOpen, toggleEllipticCurveDropdown] =
    useToggle();

  const serverInfo = useServerInfo();
  const allComponentTypes =
    serverInfo.componentTypes?.[KEY_PROVIDER_TYPE] ?? [];

  const rsaGeneratedKeySizeOptions =
    allComponentTypes[6].properties[3].options ?? [];

  const rsaGeneratedAlgorithmOptions =
    allComponentTypes[6].properties[4].options ?? [];

  const rsaEncGeneratedKeySizeOptions =
    allComponentTypes[5].properties[3].options ?? [];

  const rsaEncGeneratedAlgorithmOptions =
    allComponentTypes[5].properties[4].options ?? [];

  return (
    <>
      <FormGroup
        label={t("keySize")}
        fieldId="kc-rsa-generated-keysize"
        labelIcon={
          <HelpItem
            helpText="realm-settings-help:keySize"
            fieldLabelId="realm-settings:keySize"
          />
        }
      >
        <Controller
          name="config.keySize"
          control={control}
          defaultValue={isEnc ? ["4096"] : ["2048"]}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-rsa-generated-keysize"
              onToggle={toggleKeySizeDropdown}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                toggleKeySizeDropdown();
              }}
              selections={[value.toString()]}
              isOpen={isKeySizeDropdownOpen}
              variant={SelectVariant.single}
              aria-label={t("KeySize")}
              data-testid="select-secret-size"
            >
              {(isEnc
                ? rsaEncGeneratedKeySizeOptions
                : rsaGeneratedKeySizeOptions
              ).map((item) => (
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
          defaultValue={isEnc ? ["RSA-OAEP"] : ["RS256"]}
          render={({ onChange, value }) => (
            <Select
              toggleId="kc-elliptic"
              onToggle={toggleEllipticCurveDropdown}
              onSelect={(_, value) => {
                onChange([value.toString()]);
                toggleEllipticCurveDropdown();
              }}
              selections={[value.toString()]}
              variant={SelectVariant.single}
              aria-label={t("algorithm")}
              isOpen={isEllipticCurveDropdownOpen}
            >
              {(isEnc
                ? rsaEncGeneratedAlgorithmOptions
                : rsaGeneratedAlgorithmOptions
              ).map((p, idx) => (
                <SelectOption selected={p === value} key={idx} value={p} />
              ))}
            </Select>
          )}
        />
      </FormGroup>
    </>
  );
}
