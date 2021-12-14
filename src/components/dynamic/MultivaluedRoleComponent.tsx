import React, { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { sortedUniq } from "lodash";
import {
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";

import type RoleRepresentation from "@keycloak/keycloak-admin-client/lib/defs/roleRepresentation";
import type { ComponentProps } from "./components";
import type { MultiLine } from "../multi-line-input/multi-line-convert";
import { useAdminClient, useFetch } from "../../context/auth/AdminClient";
import { HelpItem } from "../help-enabler/HelpItem";
import { useWhoAmI } from "../../context/whoami/WhoAmI";

export const MultivaluedRoleComponent = ({
  name,
  label,
  helpText,
}: ComponentProps) => {
  const { t } = useTranslation("dynamic");
  const { whoAmI } = useWhoAmI();
  const fieldName = `config.${name}`;

  const adminClient = useAdminClient();
  const { control } = useFormContext();

  const [clientRoles, setClientRoles] = useState<RoleRepresentation[]>([]);
  const [open, setOpen] = useState(false);

  useFetch(
    async () => {
      const clients = await adminClient.clients.find();
      const clientRoles = await Promise.all(
        clients.map(async (client) => {
          const roles = await adminClient.clients.listRoles({ id: client.id! });

          return roles.map<RoleRepresentation>((role) => ({
            ...role,
          }));
        })
      );

      return clientRoles.flat();
    },
    (clientRoles) => {
      setClientRoles(clientRoles);
    },
    []
  );

  const alphabetizedClientRoles = sortedUniq(
    clientRoles.map((item) => item.name)
  ).sort((a, b) =>
    a!.localeCompare(b!, whoAmI.getLocale(), { ignorePunctuation: true })
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
        name={fieldName}
        defaultValue={[]}
        control={control}
        rules={{ required: true }}
        render={({ onChange, value }) => (
          <Select
            onToggle={(isExpanded) => setOpen(isExpanded)}
            isOpen={open}
            className="kc-role-select"
            data-testid="multivalued-role-select"
            variant={SelectVariant.typeaheadMulti}
            placeholderText={t("selectARole")}
            chipGroupProps={{
              numChips: 5,
              expandedText: t("common:hide"),
              collapsedText: t("common:showRemaining"),
            }}
            typeAheadAriaLabel={t("selectARole")}
            selections={value.map((v: MultiLine) => v.value)}
            isCreatable
            onSelect={(_, v) => {
              const option = v.toString();
              if (value.map((v: MultiLine) => v.value).includes(option)) {
                onChange(
                  value.filter((item: MultiLine) => item.value !== option)
                );
              } else {
                onChange([...value, { value: option }]);
              }
            }}
            maxHeight={200}
            onClear={() => onChange([])}
          >
            {alphabetizedClientRoles.map((option) => (
              <SelectOption key={option} value={option} />
            ))}
          </Select>
        )}
      />
    </FormGroup>
  );
};
