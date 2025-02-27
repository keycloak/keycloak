import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type { GroupQuery } from "@keycloak/keycloak-admin-client/lib/resources/groups";
import {
  SelectControl,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../../components/dynamic/components";

type GroupSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: `${SelectVariant}`;
};

export const GroupSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  required = false,
  variant = "typeahead",
}: GroupSelectProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const [groups, setGroups] = useState<GroupRepresentation[]>([]);
  const [search, setSearch] = useState("");

  useFetch(
    () => {
      const params: GroupQuery = {
        max: 20,
      };
      if (search) {
        params.search = search;
      }
      return adminClient.groups.find(params);
    },
    (groups) => setGroups(groups),
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
      options={groups.map(({ id, name }) => ({
        key: id!,
        value: name!,
        label: name,
      }))}
    />
  );
};
