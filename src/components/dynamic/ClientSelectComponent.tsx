import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useFormContext } from "react-hook-form";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { HelpItem } from "../help-enabler/HelpItem";
import type { ComponentProps } from "./components";

export const ClientSelectComponent = ({
  name,
  label,
  helpText,
  defaultValue,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { control } = useFormContext();

  const [open, setOpen] = useState(false);
  const [clients, setClients] = useState<JSX.Element[]>();
  const [search, setSearch] = useState("");

  const adminClient = useAdminClient();

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
    (clients) =>
      setClients(
        clients.map((option) => (
          <SelectOption key={option.id} value={option.clientId} />
        ))
      ),
    [search]
  );

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId={`dynamic:${label}`} />
      }
      fieldId={name!}
    >
      <Controller
        name={`config.${name}`}
        defaultValue={defaultValue || ""}
        control={control}
        render={({ onChange, value }) => (
          <Select
            toggleId={name}
            variant={SelectVariant.typeahead}
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={value}
            onFilter={(_, value) => {
              setSearch(value);
              return clients;
            }}
            onSelect={(_, value) => {
              onChange(value.toString());
              setOpen(false);
            }}
            aria-label={t(label!)}
          >
            {clients}
          </Select>
        )}
      />
    </FormGroup>
  );
};
