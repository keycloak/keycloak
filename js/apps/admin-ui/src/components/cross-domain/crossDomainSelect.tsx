import type CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";
import { HelpItem } from "@keycloak/keycloak-ui-shared";

import { useEffect, useState } from "react";

import {
  FormGroup,
  Select,
  SelectOption,
  SelectList,
  HelperText,
  HelperTextItem,
  FormHelperText,
  ValidatedOptions,
  MenuToggle,
  MenuToggleElement,
  TextInputGroup,
  TextInputGroupMain,
  ChipGroup,
  Chip,
} from "@patternfly/react-core";
import {
  Controller,
  ControllerRenderProps,
  FieldValues,
  useFormContext,
} from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAdminClient } from "../../admin-client";
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
  const { adminClient } = useAdminClient();
  const { realm: realmName } = useRealm();
  const [open, setOpen] = useState(false);
  const [filterValue, setFilterValue] = useState("");
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
      setTrustedDomains(
        deserializeCrossDomainTrustConfig(realm.attributes["crossDomainTrust"]),
      );
    },
    [],
  );

  const toggle = (
    field: ControllerRenderProps<FieldValues, string>,
    toggleRef: React.Ref<MenuToggleElement>,
  ) => (
    <MenuToggle
      variant="typeahead"
      onClick={() => setOpen(!open)}
      innerRef={toggleRef}
      isExpanded={open}
      isFullWidth
    >
      <TextInputGroup isPlain>
        <TextInputGroupMain
          value={filterValue}
          onClick={() => setOpen(!open)}
          onChange={(_, value) => setFilterValue(value)}
          isExpanded={open}
        >
          <ChipGroup>
            {selectedDomains.map((selection, index) => (
              <Chip
                key={index}
                onClick={() => {
                  onSelect(field, selection.issuer);
                }}
              >
                {selection.issuer}
              </Chip>
            ))}
          </ChipGroup>
        </TextInputGroupMain>
      </TextInputGroup>
    </MenuToggle>
  );

  const onSelect = (
    field: ControllerRenderProps<FieldValues, string>,
    value: string,
  ) => {
    // if value already exists, remove it from the selected list, if it doesn't, then add it to the selected domains
    const updated = selectedDomains.find((c) => c.issuer == value?.toString())
      ? // remove domain from list
        selectedDomains.filter((c) => c.issuer != value?.toString())
      : // add domain to list
        [
          ...selectedDomains,
          ...trustedDomains.filter((c) => c.issuer == value?.toString()),
        ];

    // update view
    setSelectedDomains(updated);
    // update the form field value
    field.onChange(onChange(updated));

    setOpen(false);
  };

  return (
    <FormGroup
      label={t(label!)}
      isRequired={required}
      labelIcon={<HelpItem helpText={t(helpText!)} fieldLabelId={label!} />}
      fieldId={name!}
    >
      <Controller
        name={name!}
        control={control}
        rules={required ? { required: true } : {}}
        render={({ field }) => (
          <Select
            toggle={(toggleRef) => toggle(field, toggleRef)}
            onOpenChange={(open) => setOpen(open)}
            isOpen={open}
            selected={selectedDomains.map((c) => c.issuer)}
            onSelect={(_, value) => value && onSelect(field, value.toString())}
          >
            <SelectList isAriaMultiselectable>
              {Object.values(trustedDomains)
                .filter(
                  (option) =>
                    filterValue.length == 0 ||
                    option.issuer.includes(filterValue),
                )
                .map((option) => (
                  <SelectOption key={option.issuer} value={option.issuer}>
                    {option.issuer}
                  </SelectOption>
                ))}
            </SelectList>
          </Select>
        )}
      />
      {errors[name!] && (
        <FormHelperText>
          <HelperText>
            <HelperTextItem variant={ValidatedOptions.error}>
              {t("required")}
            </HelperTextItem>
          </HelperText>
        </FormHelperText>
      )}
    </FormGroup>
  );
};
