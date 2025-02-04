import { NetworkError } from "@keycloak/keycloak-admin-client";
import { KeycloakDataTable } from "@keycloak/keycloak-ui-shared";
import {
  Badge,
  Modal,
  ModalVariant,
  Popover,
  Text,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { translationFormatter } from "../../utils/translationFormatter";
import {
  AddRealm,
  RealmNameRepresentation,
  RealmSelector,
} from "./RealmSelector";

export const RealmPanel = () => {
  const { t } = useTranslation();
  const { whoAmI } = useWhoAmI();
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();

  const [open, setOpen] = useState(false);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      return await fetchAdminUI<RealmNameRepresentation[]>(
        adminClient,
        "ui-ext/realms/names",
        { first: `${first}`, max: `${max}`, search: search || "" },
      );
    } catch (error) {
      if (error instanceof NetworkError && error.response.status < 500) {
        return [];
      }

      throw error;
    }
  };

  return (
    <>
      <Toolbar>
        <ToolbarContent>
          <ToolbarItem>
            <Text>{t("currentRealm")}</Text>
          </ToolbarItem>
          <ToolbarItem>
            <RealmSelector onViewAll={() => setOpen(true)} />
          </ToolbarItem>
          <ToolbarItem>
            {whoAmI.canCreateRealm() && (
              <AddRealm onClick={() => setOpen(false)} />
            )}
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
      <Modal
        variant={ModalVariant.medium}
        title={t("selectRealm")}
        isOpen={open}
        onClose={() => setOpen(false)}
      >
        <KeycloakDataTable
          loader={loader}
          isPaginated
          ariaLabelKey="selectRealm"
          searchPlaceholderKey="search"
          columns={[
            {
              name: "name",
              transforms: [cellWidth(40)],
              cellRenderer: ({ name }) =>
                name !== realm ? (
                  <Link
                    to={toDashboard({ realm: name })}
                    onClick={() => setOpen(false)}
                  >
                    {name}{" "}
                  </Link>
                ) : (
                  <Popover
                    bodyContent={t("currentRealmExplain")}
                    triggerAction="hover"
                  >
                    <>
                      {name} <Badge isRead>{t("currentRealm")}</Badge>
                    </>
                  </Popover>
                ),
            },
            {
              name: "displayName",
              transforms: [cellWidth(60)],
              cellFormatters: [translationFormatter(t)],
            },
          ]}
        />
      </Modal>
    </>
  );
};
