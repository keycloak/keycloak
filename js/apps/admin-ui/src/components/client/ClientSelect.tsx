import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { HelpItem } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import type { ComponentProps } from "../dynamic/components";

type ClientSelectProps = ComponentProps & {};

export const ClientSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  required = false,
}: ClientSelectProps) => {
  const { t } = useTranslation();
  const {
    control,
    formState: { errors },
  } = useFormContext();

  const [open, setOpen] = useState(false);
  const [clients, setClients] = useState<ClientRepresentation[]>([]);
  const [search, setSearch] = useState("");

  useFetch(
    () => {
      const params: ClientQuery = {
        max: 20,
      };
      if (search) {
        params.clientId = search;
        params.search = true;
      }
      return adminClient.clients.find(params);
    },
    (clients) => setClients(clients),
    [search],
  );

  const convert = (clients: ClientRepresentation[]) => [
    <SelectOption key="empty" value="">
      {t("none")}
    </SelectOption>,
    ...clients.map((option) => (
      <SelectOption key={option.id} value={option.clientId} />
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
        defaultValue={defaultValue || ""}
        control={control}
        rules={required ? { required: true } : {}}
        render={({ field }) => (
          <Select
            toggleId={name}
            variant={SelectVariant.typeahead}
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            isDisabled={isDisabled}
            selections={field.value}
            onFilter={(_, value) => {
              setSearch(value);
              return convert(clients);
            }}
            onSelect={(_, value) => {
              field.onChange(value.toString());
              setOpen(false);
            }}
            typeAheadAriaLabel={t(label!)}
          >
            {convert(clients)}
          </Select>
        )}
      />
    </FormGroup>
  );
};
