import type ClientRepresentation from "@keycloak/keycloak-admin-client/lib/defs/clientRepresentation";
import {
  HelpItem,
  SelectVariant,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { Button, FormGroup } from "@patternfly/react-core";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../../admin-client";
import type { ComponentProps } from "../dynamic/components";
import { ClientSelectModal } from "./ClientSelectModal";

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
}: ClientSelectProps) => {
  const { adminClient } = useAdminClient();
  const { t } = useTranslation();

  const [selectedClients, setSelectedClients] = useState<
    ClientRepresentation[]
  >([]);
  const [open, setOpen] = useState(false);

  const { control, setValue } = useFormContext();
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
      setSelectedClients(clients.filter((c) => !!c));
    },
    [],
  );

  const isSelectOne = variant === "typeahead";

  const select = (clients: ClientRepresentation[]) => {
    setSelectedClients(clients);
    if (isSelectOne) {
      setValue(name!, clients[0][clientKey]);
    } else
      setValue(
        name!,
        clients.map((c) => c[clientKey]),
      );
  };

  return (
    <FormGroup
      label={t(label!)}
      fieldId={name}
      labelIcon={
        helpText ? (
          <HelpItem helpText={t(helpText)} fieldLabelId={name!} />
        ) : undefined
      }
      isRequired={isRequired}
    >
      <Button
        variant="secondary"
        isDisabled={isDisabled}
        onClick={() => setOpen(true)}
      >
        {t("selectClient")}
      </Button>
      {open && (
        <ClientSelectModal
          isRadio={variant === "typeahead"}
          onClose={() => setOpen(false)}
          onSelect={select}
        />
      )}
      {selectedClients.length > 0 && (
        <Table>
          <Thead>
            <Tr>
              <Th>{t(clientKey)}</Th>
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
                    icon={<MinusCircleIcon />}
                    onClick={() => {
                      if (isSelectOne) {
                        setValue(name!, client[clientKey]);
                      } else
                        setValue(
                          name!,
                          value.filter(
                            (id: string) => id !== client[clientKey],
                          ),
                        );
                      setSelectedClients(
                        selectedClients.filter(
                          (c) => c[clientKey] !== client[clientKey],
                        ),
                      );
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
