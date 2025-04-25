import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import {
  SelectControl,
  SelectControlOption,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../dynamic/components";
import { PermissionsConfigurationTabsParams } from "../../permissions-configuration/routes/PermissionsConfigurationTabs";
import { useParams } from "react-router-dom";
import { useFormContext, useWatch } from "react-hook-form";

type ClientSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: `${SelectVariant}`;
  isRequired?: boolean;
  clientKey?: keyof ClientRepresentation;
  placeholderText?: string;
};

export const ClientSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  isRequired,
  variant = "typeahead",
  clientKey = "clientId",
  placeholderText,
}: ClientSelectProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const [clients, setClients] = useState<ClientRepresentation[]>([]);
  const [selectedClients, setSelectedClients] =
    useState<SelectControlOption[]>();
  const [search, setSearch] = useState("");
  const { tab } = useParams<PermissionsConfigurationTabsParams>();

  const { control } = useFormContext();
  const value = useWatch({
    control,
    name: name!,
    defaultValue: defaultValue || "",
  });

  const getValue = (): string[] => {
    if (typeof value === "string") {
      return [value];
    }
    return value || [];
  };

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

  useFetch(
    () => {
      const values = getValue().map(async (clientId) => {
        if (clientKey === "clientId") {
          return (await adminClient.clients.find({ clientId }))[0];
        } else {
          return adminClient.clients.findOne({ id: clientId });
        }
      });
      return Promise.all(values);
    },
    (clients) => {
      setSelectedClients(
        clients
          .filter((client) => !!client)
          .map((client) => ({
            key: client[clientKey] as string,
            value: client.clientId!,
          })),
      );
    },
    [],
  );

  return (
    <SelectControl
      name={name!}
      label={tab !== "evaluation" ? t(label!) : t("client")}
      labelIcon={tab !== "evaluation" ? t(helpText!) : t("selectClient")}
      controller={{
        defaultValue: defaultValue || "",
        rules: {
          required: {
            value: isRequired || false,
            message: t("required"),
          },
        },
      }}
      onFilter={(value) => setSearch(value)}
      variant={variant}
      isDisabled={isDisabled}
      selectedOptions={selectedClients}
      options={clients.map((client) => ({
        key: client[clientKey] as string,
        value: client.clientId!,
      }))}
      placeholderText={placeholderText}
    />
  );
};
