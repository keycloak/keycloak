import { HelpItem, useFetch } from "@keycloak/keycloak-ui-shared";

import { useTranslation } from "react-i18next";
import { Button, FormGroup } from "@patternfly/react-core";
import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { ComponentProps } from "../../components/dynamic/components";
import { useAdminClient } from "../../admin-client";
import { OrganizationModal } from "../../organizations/OrganizationModal";

type OrganizationSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: "typeahead" | "typeaheadMulti";
  isRequired?: boolean;
};

const convertOrganizations = (
  organizations: OrganizationRepresentation[],
): string[] => organizations.map(({ id }) => id!);

export const OrganizationSelect = ({
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  isRequired,
  name,
  variant = "typeaheadMulti",
}: OrganizationSelectProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const { control, setValue, getValues } = useFormContext();

  const values: string[] = getValues(name!) || [];
  const [open, setOpen] = useState(false);
  const [organizations, setOrganizations] = useState<
    OrganizationRepresentation[]
  >([]);

  useFetch(
    () => {
      if (values.length > 0) {
        return Promise.all(
          (values as string[]).map((id) =>
            adminClient.organizations.findOne({ id }),
          ),
        );
      }
      return Promise.resolve([]);
    },
    (organizations) => {
      setOrganizations(organizations as OrganizationRepresentation[]);
    },
    [],
  );

  const selectOne = variant === "typeahead";

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        <HelpItem helpText={t(helpText!)} fieldLabelId="organizations" />
      }
      fieldId="organizations"
      isRequired={isRequired}
    >
      <Controller
        name={name!}
        control={control}
        defaultValue={defaultValue}
        rules={{
          validate: (value?: string[]) =>
            !isRequired || (value && value.length > 0),
        }}
        render={({ field }) => (
          <>
            {open && (
              <OrganizationModal
                mode="add"
                existingOrgs={organizations}
                onClose={() => setOpen(false)}
                onAdd={async (orgs) => {
                  // not actually async but onAdd is called assuming it is
                  if (selectOne) {
                    field.onChange(convertOrganizations(orgs));
                    setOrganizations(orgs);
                  } else {
                    field.onChange([
                      ...(field.value || []),
                      ...convertOrganizations(orgs),
                    ]);
                    setOrganizations([...organizations, ...orgs]);
                  }
                  setOpen(false);
                }}
              />
            )}
            <Button
              data-testid="select-organization-button"
              isDisabled={isDisabled}
              variant="secondary"
              onClick={() => {
                setOpen(true);
              }}
            >
              {t("addOrganizations")}
            </Button>
          </>
        )}
      />
      {organizations.length > 0 && (
        <Table variant="compact">
          <Thead>
            <Tr>
              <Th>{t("organizations")}</Th>
              <Th aria-hidden="true" />
            </Tr>
          </Thead>
          <Tbody>
            {organizations.map((organization) => (
              <Tr key={organization.id}>
                <Td>{organization.name}</Td>
                <Td>
                  <Button
                    variant="link"
                    className="keycloak__client-authorization__policy-row-remove"
                    icon={<MinusCircleIcon />}
                    onClick={() => {
                      setValue(name!, [
                        ...convertOrganizations(
                          organizations.filter(
                            ({ id }) => id !== organization.id,
                          ),
                        ),
                      ]);
                      setOrganizations([
                        ...organizations.filter(
                          ({ id }) => id !== organization.id,
                        ),
                      ]);
                    }}
                  />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
    </FormGroup>
  );
};
