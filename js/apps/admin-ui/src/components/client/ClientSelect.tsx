import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import type { ClientQuery } from "@keycloak/keycloak-admin-client/lib/resources/clients";
import { SelectProps, SelectVariant } from "@patternfly/react-core/deprecated";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { SelectControl } from "ui-shared";

import { adminClient } from "../../admin-client";
import { useFetch } from "../../utils/useFetch";
import type { ComponentProps } from "../dynamic/components";

type ClientSelectProps = ComponentProps & Pick<SelectProps, "variant">;

export const ClientSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  required = false,
  variant = SelectVariant.typeahead,
}: ClientSelectProps) => {
  const { t } = useTranslation();

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

  return (
    <SelectControl
      name={name!}
      label={t(label!)}
      labelIcon={t(helpText!)}
      controller={{
        defaultValue: defaultValue || "",
        rules: {
          required: {
            value: required,
            message: t("required"),
          },
        },
      }}
      onFilter={(value) => setSearch(value)}
      variant={variant}
      isDisabled={isDisabled}
      options={clients.map(({ id, clientId }) => ({
        key: id!,
        value: clientId!,
      }))}
    />
  );
};
