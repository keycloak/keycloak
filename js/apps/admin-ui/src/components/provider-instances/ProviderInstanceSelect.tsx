import ComponentRepresentation from "@keycloak/keycloak-admin-client/lib/defs/componentRepresentation";

import {
  Chip,
  ChipGroup,
  FormGroup,
  MenuToggle,
  MenuToggleElement,
  TextInputGroup,
  TextInputGroupMain,
  Select,
  SelectOption,
  SelectList,
  FormHelperText,
  HelperText,
  HelperTextItem,
  ValidatedOptions,
} from "@patternfly/react-core";

import {
  Controller,
  ControllerRenderProps,
  FieldValues,
  useFormContext,
  get,
} from "react-hook-form";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { HelpItem } from "@keycloak/keycloak-ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";

import { ComponentProps } from "../dynamic/components";
import { useRealm } from "../../context/realm-context/RealmContext";

type ProviderInstanceSelectProps = ComponentProps & {
  providerType: string;
  onChange?: (selected: ComponentRepresentation[]) => any;
  onLoad?: (
    loaded: any,
    options: ComponentRepresentation[],
  ) => ComponentRepresentation[];
  filterOptions?: (
    options: ComponentRepresentation[],
  ) => ComponentRepresentation[];
  getDisplayName?: (option: ComponentRepresentation) => string;
  multiSelect?: boolean;
};

export const ProviderInstanceSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  required,
  providerType,
  multiSelect = false,
  getDisplayName = (option: ComponentRepresentation) => option.id!,
  onChange = (selected: ComponentRepresentation[]): any =>
    multiSelect ? selected.map((c) => c.id!) : selected.map((c) => c.id!)[0],
  filterOptions = (
    options: ComponentRepresentation[],
  ): ComponentRepresentation[] => options,
  onLoad = (
    loaded: any,
    options: ComponentRepresentation[],
  ): ComponentRepresentation[] =>
    (Array.isArray(loaded) ? loaded : [loaded])
      .map((selected) => options.findLast((p) => p.id == selected))
      .filter((p) => p != undefined) as ComponentRepresentation[],
}: ProviderInstanceSelectProps) => {
  const { t } = useTranslation();
  const {
    control,
    watch,
    formState: { errors },
  } = useFormContext();
  const { realm } = useRealm();

  const [providerInstances, setProviderInstances] = useState<
    ComponentRepresentation[]
  >([]);
  const [selectedProviderInstances, setSelectedProviderInstances] = useState<
    ComponentRepresentation[]
  >([]);
  const [filterValue, setFilterValue] = useState("");

  const [open, setOpen] = useState(false);

  const selectedValues: any = watch(name!);

  useEffect(() => {
    if (selectedValues == undefined) return;

    setSelectedProviderInstances(onLoad(selectedValues, providerInstances));
  }, [selectedProviderInstances, providerInstances]);

  useFetch(
    async () => {
      // fetch realm
      const realmModel = await adminClient.realms.findOne({ realm });

      // search for instances of attribute store providers
      const search: { [name: string]: string | number } = {
        parentId: realmModel!.id!,
        type: providerType,
      };
      return adminClient.components.find(search);
    },
    (instances) => {
      setProviderInstances(filterOptions(instances));
    },
    [realm],
  );

  const onSelect = (
    field: ControllerRenderProps<FieldValues, string>,
    value: string,
  ) => {
    // if value already exists, remove it from the selected list, if it doesn't, then add it to the selected instances
    const updated = selectedProviderInstances.find((c) => c.id == value)
      ? // remove domain from list
        selectedProviderInstances.filter((c) => c.id != value)
      : // add domain to list
        multiSelect
        ? [
            ...selectedProviderInstances,
            ...providerInstances.filter((c) => c.id == value),
          ]
        : providerInstances.filter((c) => c.id == value);

    // update view
    setSelectedProviderInstances(updated);

    // update the form field value
    field.onChange(onChange(updated));

    setOpen(false);
  };

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
            {selectedProviderInstances.map((selection, index) => (
              <Chip
                key={index}
                onClick={() => {
                  onSelect(field, selection.id!);
                }}
              >
                {getDisplayName(selection)}
              </Chip>
            ))}
          </ChipGroup>
        </TextInputGroupMain>
      </TextInputGroup>
    </MenuToggle>
  );

  return (
    <FormGroup
      label={label}
      labelIcon={
        <HelpItem helpText={helpText} fieldLabelId="providerInstanceSelect" />
      }
      fieldId="providerInstanceSelect"
      isRequired={!!required}
    >
      <Controller
        name={name!}
        defaultValue={defaultValue}
        control={control}
        rules={
          required
            ? { validate: (value) => value != undefined && value.length > 0 }
            : { required: required }
        }
        render={({ field }) => (
          <Select
            toggle={(toggleRef) => toggle(field, toggleRef)}
            onOpenChange={(open) => setOpen(open)}
            isOpen={open}
            selected={selectedProviderInstances.map((c) => c.id)}
            onSelect={(_, value) => value && onSelect(field, value.toString())}
          >
            <SelectList>
              {Object.values(providerInstances)
                .filter(
                  (option) =>
                    filterValue.length == 0 ||
                    getDisplayName(option).includes(filterValue),
                )
                .map((option) => (
                  <SelectOption key={option.id} value={option.id}>
                    {getDisplayName(option)}
                  </SelectOption>
                ))}
            </SelectList>
          </Select>
        )}
      />
      {get(errors, name!) && (
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
