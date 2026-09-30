import type OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import {
  FormErrorText,
  HelpItem,
  KeycloakDataTable,
  ListEmptyState,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { Button, FormGroup, Modal, ModalVariant } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../../components/dynamic/components";

type OrganizationSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: "typeahead" | "typeaheadMulti";
  isRequired?: boolean;
};

export const OrganizationSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  isRequired,
  variant = "typeaheadMulti",
}: OrganizationSelectProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const {
    control,
    setValue,
    getValues,
    formState: { errors },
  } = useFormContext();
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<OrganizationRepresentation[]>([]);
  const [picked, setPicked] = useState<OrganizationRepresentation[]>([]);
  const selectOne = variant === "typeahead";

  useFetch(
    () => {
      const v = getValues(name!) ?? defaultValue;
      const values: string[] = typeof v === "string" ? (v ? [v] : []) : v || [];
      if (values.length > 0) {
        return Promise.all(
          values.map((id) => adminClient.organizations.findOne({ id })),
        );
      }
      return Promise.resolve([]);
    },
    (orgs) => setSelected(orgs),
    [],
  );

  const loader = async (first?: number, max?: number, search?: string) =>
    adminClient.organizations.find({ first, max, search });

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        helpText ? (
          <HelpItem helpText={t(helpText)} fieldLabelId="organizations" />
        ) : undefined
      }
      fieldId={name}
      isRequired={isRequired}
    >
      <Controller
        name={name!}
        control={control}
        defaultValue={defaultValue}
        rules={{
          validate: (value) => {
            if (!isRequired) return true;
            if (Array.isArray(value)) return value.length > 0;
            return !!value;
          },
        }}
        render={({ field }) => (
          <>
            {open && (
              <Modal
                data-testid="select-organization-modal"
                variant={ModalVariant.large}
                title={t("selectOrganization")}
                isOpen
                onClose={() => setOpen(false)}
                actions={[
                  <Button
                    data-testid="confirm"
                    key="confirm"
                    variant="primary"
                    isDisabled={picked.length === 0}
                    onClick={() => {
                      const ids = picked.map(({ id }) => id!);
                      if (selectOne) {
                        field.onChange(ids[0]);
                        setSelected(picked.slice(0, 1));
                      } else {
                        const existing: string[] = field.value ?? [];
                        const fresh = picked.filter(
                          ({ id }) => !existing.includes(id!),
                        );
                        field.onChange([
                          ...existing,
                          ...fresh.map((o) => o.id),
                        ]);
                        setSelected([...selected, ...fresh]);
                      }
                      setOpen(false);
                    }}
                  >
                    {t("add")}
                  </Button>,
                  <Button
                    data-testid="cancel"
                    key="cancel"
                    variant="link"
                    onClick={() => setOpen(false)}
                  >
                    {t("cancel")}
                  </Button>,
                ]}
              >
                <KeycloakDataTable
                  onSelect={(rows) => setPicked([...rows])}
                  searchPlaceholderKey="searchOrganization"
                  isPaginated
                  canSelectAll={!selectOne}
                  isRadio={selectOne}
                  loader={loader}
                  ariaLabelKey="organizations"
                  columns={[{ name: "name" }, { name: "alias" }]}
                  emptyState={
                    <ListEmptyState
                      message={t("emptyOrganizations")}
                      instructions={t("emptyOrganizationsInstructions")}
                    />
                  }
                />
              </Modal>
            )}
            <Button
              data-testid="select-organization-button"
              variant="secondary"
              isDisabled={isDisabled}
              onClick={() => {
                setPicked([]);
                setOpen(true);
              }}
            >
              {t("selectOrganization")}
            </Button>
          </>
        )}
      />
      {selected.length > 0 && (
        <Table variant="compact">
          <Thead>
            <Tr>
              <Th>{t("organization")}</Th>
              <Th aria-hidden="true" />
            </Tr>
          </Thead>
          <Tbody>
            {selected.map((org) => (
              <Tr key={org.id}>
                <Td>{org.name}</Td>
                <Td>
                  <Button
                    variant="link"
                    className="keycloak__client-authorization__policy-row-remove"
                    aria-label={t("remove")}
                    icon={<MinusCircleIcon />}
                    onClick={() => {
                      const remaining = selected.filter((o) => o.id !== org.id);
                      setSelected(remaining);
                      setValue(
                        name!,
                        selectOne ? "" : remaining.map((o) => o.id),
                        { shouldDirty: true },
                      );
                    }}
                  />
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      )}
      {errors[name!] && <FormErrorText message={t("required")} />}
    </FormGroup>
  );
};
