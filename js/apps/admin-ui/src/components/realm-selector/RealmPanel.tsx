import { NetworkError } from "@keycloak/keycloak-admin-client";
import { KeycloakDataTable, label } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Label,
  Modal,
  ModalVariant,
  Split,
  SplitItem,
  Text,
  ToolbarItem,
} from "@patternfly/react-core";
import { cellWidth } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useRealm } from "../../context/realm-context/RealmContext";
import {
  RealmNameRepresentation,
  useRecentRealms,
} from "../../context/RecentRealms";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { translationFormatter } from "../../utils/translationFormatter";
import { CheckIcon } from "@patternfly/react-icons";
import { toAddRealm } from "../../realm/routes/AddRealm";

type RealmLinkProps = {
  name: string;
  recent?: boolean;
  onNavigate: () => void;
};

const RealmLink = ({ name, recent, onNavigate }: RealmLinkProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  return (
    <Link to={toDashboard({ realm: name })} onClick={onNavigate}>
      <Split>
        <SplitItem isFilled>{name}</SplitItem>
        <SplitItem>{name === realm && <CheckIcon />}</SplitItem>
        {recent ? (
          <SplitItem>
            <Label>{t("recent")}</Label>
          </SplitItem>
        ) : null}
      </Split>
    </Link>
  );
};

type AddRealmProps = {
  onClick: () => void;
};

const AddRealm = ({ onClick }: AddRealmProps) => {
  const { realm } = useRealm();
  const { t } = useTranslation();

  return (
    <Button
      data-testid="add-realm"
      component={(props) => <Link {...props} to={toAddRealm({ realm })} />}
      onClick={onClick}
      isBlock
    >
      {t("createRealm")}
    </Button>
  );
};

type RealmResult = RealmNameRepresentation & { recent?: boolean };

export const RealmPanel = () => {
  const { t } = useTranslation();
  const { realmRepresentation: realm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const { adminClient } = useAdminClient();
  const recentRealms = useRecentRealms();

  const [open, setOpen] = useState(false);

  const loader = async (first?: number, max?: number, search?: string) => {
    try {
      const result = await fetchAdminUI<RealmResult[]>(
        adminClient,
        "ui-ext/realms/names",
        {
          first: `${first}`,
          max: `${max}`,
          search: search || "",
        },
      );
      if (first === 0) {
        return [
          ...recentRealms.map((r) => ({ recent: true, ...r })),
          ...result,
        ];
      }
      return result;
    } catch (error) {
      if (error instanceof NetworkError && error.response.status < 500) {
        return [];
      }

      throw error;
    }
  };

  return (
    <>
      <Text style={{ marginRight: "1rem" }}>
        {realm?.displayName ? (
          <strong>{label(t, realm.displayName)}</strong>
        ) : null}{" "}
        {realm?.realm}
      </Text>
      <Button variant="primary" onClick={() => setOpen((value) => !value)}>
        {t("selectRealm")}
      </Button>
      <Modal
        variant={ModalVariant.large}
        title={t("selectRealm")}
        isOpen={open}
        onClose={() => setOpen(false)}
      >
        <KeycloakDataTable
          loader={loader}
          isPaginated
          ariaLabelKey="selectRealm"
          searchPlaceholderKey="search"
          toolbarItem={
            whoAmI.canCreateRealm() ? (
              <ToolbarItem>
                <AddRealm onClick={() => setOpen(false)} />
              </ToolbarItem>
            ) : undefined
          }
          columns={[
            {
              name: "name",
              transforms: [cellWidth(40)],
              cellRenderer: ({ name, recent }) => (
                <RealmLink
                  name={name}
                  recent={recent}
                  onNavigate={() => setOpen(false)}
                />
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
