import GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import {
  SelectControl,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../../components/dynamic/components";
import { PermissionsConfigurationTabsParams } from "../routes/PermissionsConfigurationTabs";
import { useParams } from "react-router-dom";

type GroupSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: `${SelectVariant}`;
  isRequired?: boolean;
};

export const GroupSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  isRequired,
  variant = "typeahead",
}: GroupSelectProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const [groups, setGroups] = useState<GroupRepresentation[]>([]);
  const { tab } = useParams<PermissionsConfigurationTabsParams>();

  useFetch(
    () => {
      return adminClient.groups.find();
    },
    (groups) => setGroups(groups),
    [],
  );

  return (
    <SelectControl
      name={name!}
      label={tab !== "evaluation" ? t(label!) : t("group")}
      labelIcon={tab !== "evaluation" ? t(helpText!) : t("selectGroup")}
      controller={{
        defaultValue: defaultValue || "",
        rules: {
          required: {
            value: isRequired || false,
            message: t("required"),
          },
        },
      }}
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
