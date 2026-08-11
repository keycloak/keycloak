import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  FormErrorText,
  HelpItem,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { Button, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../dynamic/components";
import { ClientSelectModal } from "./ClientSelectModal";

type ClientKey = "id" | "clientId";

type SelectedClient = Pick<ClientRepresentation, ClientKey>;

type ClientSelectProps = Omit<ComponentProps, "convertToName"> & {
  variant?: `${SelectVariant}`;
  isRequired?: boolean;
  clientKey?: ClientKey;
};

export const ClientSelect = ({
  name,
  label,
  helpText,
  defaultValue,
  isDisabled = false,
  required,
  isRequired = required,
  variant = "typeahead",
  clientKey = "clientId",
}: ClientSelectProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();
  const {
    control,
    setValue,
    getValues,
    formState: { errors },
  } = useFormContext();

  const [open, setOpen] = useState(false);
  const [selectedClients, setSelectedClients] = useState<SelectedClient[]>([]);

  const isSelectOne = variant === "typeahead";

  useFetch(
    () => {
      const v = getValues(name!) ?? defaultValue;
      const values: string[] = typeof v === "string" ? (v ? [v] : []) : v || [];
      if (values.length > 0) {
        return Promise.all(
          values.map(async (val) => {
            if (clientKey === "clientId") {
              const clients = await adminClient.clients.find({
                clientId: val,
              });
              return clients[0];
            }
            return adminClient.clients.findOne({ id: val });
          }),
        );
      }
      return Promise.resolve([]);
    },
    (clients) => {
      setSelectedClients(
        clients
          .filter((c) => !!c)
          .map(({ id, clientId }) => ({ id, clientId })),
      );
    },
    [],
  );

  return (
    <FormGroup
      label={t(label!)}
      labelIcon={
        helpText ? (
          <HelpItem helpText={t(helpText)} fieldLabelId={name!} />
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
              <ClientSelectModal
                isRadio={isSelectOne}
                onClose={() => setOpen(false)}
                onSelect={(clients) => {
                  if (isSelectOne) {
                    field.onChange(clients[0][clientKey]);
                    setSelectedClients(
                      clients.map(({ id, clientId }) => ({ id, clientId })),
                    );
                  } else {
                    const newIds = clients.map((c) => c[clientKey] as string);
                    const existing: string[] = field.value ?? [];
                    field.onChange([...existing, ...newIds]);
                    setSelectedClients((prev) => [
                      ...prev,
                      ...clients.map(({ id, clientId }) => ({ id, clientId })),
                    ]);
                  }
                }}
              />
            )}
            <Button
              data-testid="select-client-button"
              variant="secondary"
              isDisabled={isDisabled}
              onClick={() => setOpen(true)}
            >
              {t("selectClient")}
            </Button>
          </>
        )}
      />
      {selectedClients.length > 0 && (
        <Table variant="compact">
          <Thead>
            <Tr>
              <Th>{t("clientId")}</Th>
              <Th aria-hidden="true" />
            </Tr>
          </Thead>
          <Tbody>
            {selectedClients.map((client) => (
              <Tr key={client.id}>
                <Td>{client.clientId}</Td>
                <Td>
                  <Button
                    variant="link"
                    className="keycloak__client-authorization__policy-row-remove"
                    aria-label={t("remove")}
                    icon={<MinusCircleIcon />}
                    onClick={() => {
                      const remaining = selectedClients.filter(
                        (c) => c[clientKey] !== client[clientKey],
                      );
                      setSelectedClients(remaining);
                      if (isSelectOne) {
                        setValue(name!, "");
                      } else {
                        setValue(
                          name!,
                          remaining.map((c) => c[clientKey] as string),
                        );
                      }
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
