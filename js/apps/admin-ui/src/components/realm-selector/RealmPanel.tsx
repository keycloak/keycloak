import { NetworkError } from "@keycloak/keycloak-admin-client";
import {
  KeycloakDataTable,
  label,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Flex,
  FlexItem,
  Icon,
  Modal,
  ModalVariant,
  Stack,
  StackItem,
  ToolbarItem,
} from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";
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
import { AddRealm, RealmNameRepresentation } from "./RealmSelector";

export const RealmPanel = () => {
  const { t } = useTranslation();
  const { realmRepresentation: realm } = useRealm();
  const { whoAmI } = useWhoAmI();
  const { adminClient } = useAdminClient();

  const [show, setShow] = useState(false);
  const [open, setOpen] = useState(false);

  useFetch(
    async () => {
      try {
        return await fetchAdminUI<RealmNameRepresentation[]>(
          adminClient,
          "ui-ext/realms/names",
          { first: "0", max: "11" },
        );
      } catch (error) {
        if (error instanceof NetworkError && error.response.status < 500) {
          return [];
        }

        throw error;
      }
    },
    (realms) => setShow(realms.length === 11),
    [realm?.realm],
  );

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

  if (!show) {
    return undefined;
  }

  return (
    <>
      <Button variant="control" onClick={() => setOpen((value) => !value)}>
        <Flex alignItems={{ default: "alignItemsCenter" }}>
          <FlexItem>
            <Stack>
              {realm?.displayName ? (
                <StackItem className="pf-v5-u-font-weight-bold">
                  {label(t, realm.displayName)}
                </StackItem>
              ) : null}
              <StackItem isFilled>{realm?.realm}</StackItem>
            </Stack>
          </FlexItem>
          <FlexItem>
            <Icon>
              <CaretDownIcon />
            </Icon>
          </FlexItem>
        </Flex>
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
              cellRenderer: ({ name }) => (
                <Link
                  to={toDashboard({ realm: name })}
                  onClick={() => setOpen(false)}
                >
                  {name}
                </Link>
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
