import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
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
import { Fragment, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import {
  fetchUsedBy,
  type UsedByClientRef as FlowUsedByRow,
} from "../../components/role-mapping/resource";
import { useRealm } from "../../context/realm-context/RealmContext";
import { toClient } from "../../clients/routes/Client";
import useToggle from "../../utils/useToggle";
import { AuthenticationType, REALM_FLOWS } from "../constants";

import style from "./used-by.module.css";

const ClientUsedByLink = ({
  id,
  clientId,
}: {
  id?: string;
  clientId: string;
}) => {
  const { realm } = useRealm();
  const label = <strong>{clientId}</strong>;
  if (!id) {
    return label;
  }
  return (
    <Link
      to={toClient({
        realm,
        clientId: id,
        tab: "settings",
      })}
    >
      {label}
    </Link>
  );
};

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

const UsedByModal = ({ id, isSpecificClient, onClose }: UsedByModalProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();

  const loader = async (
    first?: number,
    max?: number,
    search?: string,
  ): Promise<FlowUsedByRow[]> => {
    return fetchUsedBy(adminClient, {
      id,
      type: isSpecificClient ? "clients" : "idp",
      first: first || 0,
      max: max || 10,
      search,
    });
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
          isSpecificClient
            ? {
                name: "name",
                displayKey: "name",
                cellRenderer: (row: FlowUsedByRow) => (
                  <ClientUsedByLink
                    id={row.id ?? undefined}
                    clientId={row.clientId}
                  />
                ),
              }
            : {
                name: "name",
                cellRenderer: (row: FlowUsedByRow) => (
                  <strong>{row.clientId}</strong>
                ),
              },
        ]}
      />
    </Modal>
  );
};

export const UsedBy = ({ authType: { id, usedBy } }: UsedByProps) => {
  const { t } = useTranslation();
  const { realmRepresentation: realm } = useRealm();
  const [open, toggle] = useToggle();

  const clientRefs = usedBy?.clientRefs;
  const clientsOrFallback: FlowUsedByRow[] = useMemo(() => {
    if (!usedBy || usedBy.type !== "SPECIFIC_CLIENTS") {
      return [];
    }
    if (clientRefs && clientRefs.length > 0) {
      return clientRefs;
    }
    return usedBy.values.map((clientId) => ({ clientId }));
  }, [usedBy, clientRefs]);

  const key = Object.entries(realm!).find(
    (e) => e[1] === usedBy?.values[0],
  )?.[0];

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
                {usedBy.type === "SPECIFIC_CLIENTS"
                  ? clientsOrFallback.map((ref, index) => (
                      <Fragment key={`${id}-used-${ref.clientId}-${index}`}>
                        <ClientUsedByLink
                          id={ref.id ?? undefined}
                          clientId={ref.clientId}
                        />
                        {index < clientsOrFallback.length - 1 ? ", " : ""}
                      </Fragment>
                    ))
                  : usedBy.values.map((used, index) => (
                      <Fragment key={`${id}-used-${used}-${index}`}>
                        <strong>{used}</strong>
                        {index < usedBy.values.length - 1 ? ", " : ""}
                      </Fragment>
                    ))}
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
