import type CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useEffect, useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import type { ComponentProps } from "../dynamic/components";
import { useRealm } from "../../context/realm-context/RealmContext";
import { deserializeCrossDomainTrustConfig } from "../../realm-settings/utils/crossDomainTrust";

type CrossDomainSelectProps = ComponentProps & {
  onLoad?: (
    trustedDomains: CrossDomainTrustConfig[],
    data: any,
  ) => CrossDomainTrustConfig[];
  onChange?: (selectedDomains: CrossDomainTrustConfig[]) => any;
};

export const CrossDomainSelect = ({
  name,
  label,
  helpText,
  isDisabled = false,
  required = false,
  onChange = (d) => d,
  onLoad = (_, d) => d,
}: CrossDomainSelectProps) => {
  const { t } = useTranslation();
  const {
    control,
    formState: { errors },
    watch,
  } = useFormContext();
  const { realm: realmName } = useRealm();
  const [open, setOpen] = useState(false);
  const [trustedDomains, setTrustedDomains] = useState<
    CrossDomainTrustConfig[]
  >([]);
  const [selectedDomains, setSelectedDomains] = useState<
    CrossDomainTrustConfig[]
  >([]);

  // watch currrent form value, needed for initialization
  const formValue = watch(name!);

  // initialize state of selector
  useEffect(() => {
    const loaded = onLoad(trustedDomains, formValue);
    setSelectedDomains(loaded);
  }, [formValue, trustedDomains]);

  // fetch configs from realm
  useFetch(
    () => adminClient.realms.findOne({ realm: realmName }),
    (realm) => {
      if (!realm?.attributes) return;
      setTrustedDomains(deserializeCrossDomainTrustConfig(realm.attributes));
    },
    [],
  );

  // convert configs to select options
  const convert = (options: CrossDomainTrustConfig[]) => [
    <SelectOption key="empty" value="">
      {t("none")}
    </SelectOption>,
    ...Object.values(options).map((option) => (
      <SelectOption key={option.issuer} value={option.issuer} />
    )),
  ];

  return (
    <FormGroup
      label={t(label!)}
      isRequired={required}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={label!} />}
      fieldId={name!}
      validated={errors[name!] ? "error" : "default"}
      helperTextInvalid={t("required")}
    >
      <Controller
        name={name!}
        control={control}
        rules={required ? { required: true } : {}}
        render={({ field }) => (
          <Select
            toggleId={name}
            variant={SelectVariant.typeaheadMulti}
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            isDisabled={isDisabled}
            selections={selectedDomains.map((c) => c.issuer)}
            onFilter={(_, value) => {
              return convert(
                trustedDomains.filter((c) => c.issuer.includes(value)),
              );
            }}
            onSelect={(_, value) => {
              const updated = selectedDomains.find(
                (c) => c.issuer == value.toString(),
              )
                ? selectedDomains.filter((c) => c.issuer != value.toString())
                : [
                    ...selectedDomains,
                    ...trustedDomains.filter(
                      (c) => c.issuer == value.toString(),
                    ),
                  ];
              field.onChange(onChange(updated));
              setOpen(false);
            }}
            typeAheadAriaLabel={t(label!)}
          >
            {convert(trustedDomains)}
          </Select>
        )}
      />
    </FormGroup>
  );
};
