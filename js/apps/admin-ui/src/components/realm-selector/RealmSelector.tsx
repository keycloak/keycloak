import { NetworkError } from "@keycloak/keycloak-admin-client";
import { label, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Divider,
  Dropdown,
  DropdownItem,
  DropdownList,
  Label,
  MenuToggle,
  Split,
  SplitItem,
  Stack,
  StackItem,
} from "@patternfly/react-core";
import { CheckIcon } from "@patternfly/react-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useRecentRealms } from "../../context/RecentRealms";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { toAddRealm } from "../../realm/routes/AddRealm";

import "./realm-selector.css";

const MAX_RESULTS = 10;

type AddRealmProps = {
  onClick: () => void;
};

export const AddRealm = ({ onClick }: AddRealmProps) => {
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

type RealmTextProps = {
  name: string;
  displayName?: string;
  showIsRecent?: boolean;
};

const RealmText = ({ name, displayName, showIsRecent }: RealmTextProps) => {
  const { realm } = useRealm();
  const { t } = useTranslation();

  return (
    <Split>
      <SplitItem isFilled>
        <Stack>
          {displayName ? (
            <StackItem className="pf-v5-u-font-weight-bold" isFilled>
              {label(t, displayName)}
            </StackItem>
          ) : null}
          <StackItem isFilled>{name}</StackItem>
        </Stack>
      </SplitItem>
      <SplitItem>{name === realm && <CheckIcon />}</SplitItem>
      {showIsRecent ? (
        <SplitItem>
          <Label>{t("recent")}</Label>
        </SplitItem>
      ) : null}
    </Split>
  );
};

export type RealmNameRepresentation = {
  name: string;
  displayName?: string;
};

export const RealmSelector = () => {
  const { realm } = useRealm();
  const { adminClient } = useAdminClient();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [realms, setRealms] = useState<RealmNameRepresentation[]>([]);
  const { t } = useTranslation();
  const recentRealms = useRecentRealms();
  const navigate = useNavigate();

  useFetch(
    async () => {
      try {
        return await fetchAdminUI<RealmNameRepresentation[]>(
          adminClient,
          "ui-ext/realms/names",
          { first: "0", max: `${MAX_RESULTS + 1}` },
        );
      } catch (error) {
        if (error instanceof NetworkError && error.response.status < 500) {
          return [];
        }

        throw error;
      }
    },
    setRealms,
    [realm],
  );

  const sortedRealms = useMemo(
    () =>
      realms.sort((a, b) => {
        if (a.name === realm) return -1;
        if (b.name === realm) return 1;
        if (recentRealms.includes(a.name)) return -1;
        if (recentRealms.includes(b.name)) return 1;

        return a.name.localeCompare(b.name, whoAmI.getLocale());
      }),
    [recentRealms, realms],
  );

  const realmDisplayName = useMemo(
    () => realms.find((r) => r.name === realm)?.displayName,
    [realm, realms],
  );

  if (realms.length > MAX_RESULTS) {
    return undefined;
  }

  return (
    <Dropdown
      id="realm-select"
      className="keycloak__realm_selector__dropdown"
      isOpen={open}
      onOpenChange={(isOpen) => setOpen(isOpen)}
      toggle={(ref) => (
        <MenuToggle
          ref={ref}
          data-testid="realmSelector"
          onClick={() => {
            setOpen(!open);
          }}
          isFullWidth
        >
          <Stack className="keycloak__realm_selector__dropdown">
            {realmDisplayName ? (
              <StackItem className="pf-v5-u-font-weight-bold" isFilled>
                {label(t, realmDisplayName)}
              </StackItem>
            ) : null}
            <StackItem isFilled>{realm}</StackItem>
          </Stack>
        </MenuToggle>
      )}
    >
      <DropdownList>
        {sortedRealms.map((realm) => (
          <DropdownItem
            key={realm.name}
            onClick={() => {
              navigate(toDashboard({ realm: realm.name }));
              setOpen(false);
            }}
          >
            <RealmText
              {...realm}
              showIsRecent={
                realms.length > 5 && recentRealms?.includes(realm.name)
              }
            />
          </DropdownItem>
        ))}
        {whoAmI.canCreateRealm() && (
          <>
            <Divider key="divider" />
            <DropdownItem key="add" component="div">
              <AddRealm onClick={() => setOpen(false)} />
            </DropdownItem>
          </>
        )}
      </DropdownList>
    </Dropdown>
  );
};
