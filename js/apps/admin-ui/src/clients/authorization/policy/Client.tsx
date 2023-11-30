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

import { adminClient } from "../../../admin-client";
import { useFetch } from "../../../utils/useFetch";

export const Client = () => {
  const { t } = useTranslation();
  const {
    control,
    getValues,
    formState: { errors },
  } = useFormContext();
  const values: string[] | undefined = getValues("clients");

  const [open, setOpen] = useState(false);
  const [clients, setClients] = useState<ClientRepresentation[]>([]);
  const [search, setSearch] = useState("");

  useFetch(
    async () => {
      const params: ClientQuery = {
        max: 20,
      };
      if (search) {
        params.clientId = search;
        params.search = true;
      }

      if (values?.length && !search) {
        return await Promise.all(
          values.map(
            (id: string) =>
              adminClient.clients.findOne({ id }) as ClientRepresentation,
          ),
        );
      }
      return await adminClient.clients.find(params);
    },
    setClients,
    [search],
  );

  const convert = (clients: ClientRepresentation[]) =>
    clients.map((option) => (
      <SelectOption
        key={option.id!}
        value={option.id}
        selected={values?.includes(option.id!)}
      >
        {option.clientId}
      </SelectOption>
    ));

  return (
    <FormGroup
      label={t("clients")}
      labelIcon={
        <HelpItem helpText={t("policyClientHelp")} fieldLabelId="client" />
      }
      fieldId="clients"
      helperTextInvalid={t("requiredClient")}
      validated={errors.clients ? "error" : "default"}
      isRequired
    >
      <Controller
        name="clients"
        defaultValue={[]}
        control={control}
        rules={{
          validate: (value) => value.length > 0,
        }}
        render={({ field }) => (
          <Select
            toggleId="clients"
            variant={SelectVariant.typeaheadMulti}
            onToggle={(open) => setOpen(open)}
            isOpen={open}
            selections={field.value}
            onFilter={(_, value) => {
              setSearch(value);
              return convert(clients);
            }}
            onSelect={(_, v) => {
              const option = v.toString();
              if (field.value.includes(option)) {
                field.onChange(
                  field.value.filter((item: string) => item !== option),
                );
              } else {
                field.onChange([...field.value, option]);
              }
              setOpen(false);
            }}
            aria-label={t("clients")}
          >
            {convert(clients)}
          </Select>
        )}
      />
    </FormGroup>
  );
};
