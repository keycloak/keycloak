import { KeycloakDataTable, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Modal,
  ModalVariant,
  Popover,
  Text,
  TextContent,
  TextVariants,
} from "@patternfly/react-core";
import { CheckCircleIcon } from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { useAdminClient } from "../../admin-client";
import { fetchUsedBy } from "../../components/role-mapping/resource";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toClient } from "../../clients/routes/Client";
import useToggle from "../../utils/useToggle";
import { AuthenticationType, REALM_FLOWS } from "../constants";

import style from "./used-by.module.css";

type UsedByProps = {
  authType: AuthenticationType;
};

const Label = ({ label }: { label: string }) => (
  <>
    <CheckCircleIcon className={style.label} /> {label}
  </>
);

type UsedByModalProps = {
  id: string;
  onClose: () => void;
  isSpecificClient: boolean;
};

type ClientInfo = {
  name: string;
  id?: string;
};

const UsedByModal = ({ id, isSpecificClient, onClose }: UsedByModalProps) => {
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const { t } = useTranslation();

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<ClientInfo[]> => {
    const result = await fetchUsedBy(adminClient, {
      id,
      type: isSpecificClient ? "clients" : "idp",
      first: first || 0,
      max: max || 10,
      search,
    });
    if (isSpecificClient) {
      return await Promise.all(
          result.map(async (clientName) => {
            try {
              const clients = await adminClient.clients.find({
                clientId: clientName,
                realm: realm,
              });
              const client = clients[0];
              return {name: clientName, id: client?.id};
            } catch {
              return {name: clientName};
            }
          }),
      );
    }
    return result.map((p) => ({ name: p }));
  };

  return (
    <Modal
      header={
        <TextContent>
          <Text component={TextVariants.h1}>{t("flowUsedBy")}</Text>
          <Text>
            {t("flowUsedByDescription", {
              value: isSpecificClient ? t("clients") : t("identiyProviders"),
            })}
          </Text>
        </TextContent>
      }
      variant={ModalVariant.medium}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid="cancel"
          id="modal-cancel"
          key="cancel"
          onClick={onClose}
        >
          {t("close")}
        </Button>,
      ]}
    >
      <KeycloakDataTable
        loader={loader}
        isPaginated
        ariaLabelKey="usedBy"
        searchPlaceholderKey="search"
        columns={[
          {
            name: "name",
            displayKey: "name",
            cellRenderer: (client: ClientInfo) => {
              if (isSpecificClient && client.id) {
                return (
                  <Link
                    to={toClient({
                      realm,
                      clientId: client.id,
                      tab: "settings",
                    })}
                  >
                    {client.name}
                  </Link>
                );
              }
              return client.name;
            },
          },
        ]}
      />
    </Modal>
  );
};

export const UsedBy = ({ authType: { id, usedBy } }: UsedByProps) => {
  const { t } = useTranslation();
  const { realmRepresentation: realm, realm: realmName } = useRealm();
  const { adminClient } = useAdminClient();
  const [open, toggle] = useToggle();
  const [clientDetails, setClientDetails] = useState<
    { clientId: string; id: string | undefined }[]
  >([]);

  const key = Object.entries(realm!).find(
    (e) => e[1] === usedBy?.values[0],
  )?.[0];

  useFetch(
    async () => {
      if (usedBy?.type === "SPECIFIC_CLIENTS" && usedBy.values.length > 0) {
        return await Promise.all(
            usedBy.values.map(async (clientId) => {
              try {
                const clients = await adminClient.clients.find({
                  clientId: clientId,
                  realm: realmName,
                });
                const client = clients[0];
                return {clientId, id: client?.id};
              } catch {
                return {clientId, id: undefined};
              }
            }),
        );
      }
      return [];
    },
    (details) => setClientDetails(details),
    [usedBy?.type, usedBy?.values, realmName],
  );

  return (
    <>
      {open && (
        <UsedByModal
          id={id!}
          onClose={toggle}
          isSpecificClient={usedBy?.type === "SPECIFIC_CLIENTS"}
        />
      )}
      {(usedBy?.type === "SPECIFIC_PROVIDERS" ||
        usedBy?.type === "SPECIFIC_CLIENTS") &&
        (usedBy.values.length <= 8 ? (
          <Popover
            key={id}
            aria-label={t("usedBy")}
            bodyContent={
              <div key={`usedBy-${id}-${usedBy.values}`}>
                {t(
                  "appliedBy" +
                    (usedBy.type === "SPECIFIC_CLIENTS"
                      ? "Clients"
                      : "Providers"),
                )}{" "}
                {usedBy.values.map((used, index) => {
                  if (usedBy.type === "SPECIFIC_CLIENTS") {
                    const clientDetail = clientDetails?.find(
                      (c: { clientId: string; id: string | undefined }) =>
                        c.clientId === used,
                    );
                    return (
                      <span key={`${used}-${index}`}>
                        {clientDetail?.id ? (
                          <Link
                            to={toClient({
                              realm: realmName,
                              clientId: clientDetail.id,
                              tab: "settings",
                            })}
                          >
                            {used}
                          </Link>
                        ) : (
                          <strong>{used}</strong>
                        )}
                        {index < usedBy.values.length - 1 ? ", " : ""}
                      </span>
                    );
                  } else {
                    return (
                      <span key={`${used}-${index}`}>
                        <strong>{used}</strong>
                        {index < usedBy.values.length - 1 ? ", " : ""}
                      </span>
                    );
                  }
                })}
              </div>
            }
          >
            <Button variant="link" className={style.label}>
              <Label label={t(`used.${usedBy.type}`)} />
            </Button>
          </Popover>
        ) : (
          <Button variant="link" className={style.label} onClick={toggle}>
            <Label label={t(`used.${usedBy.type}`)} />
          </Button>
        ))}
      {usedBy?.type === "DEFAULT" && (
        <Label label={t(`flow.${REALM_FLOWS.get(key!)}`)} />
      )}
      {!usedBy?.type && t("used.notInUse")}
    </>
  );
};
