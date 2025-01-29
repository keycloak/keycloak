import { NetworkError } from "@keycloak/keycloak-admin-client";
import { label, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Divider,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  DropdownList,
  MenuToggle,
  Split,
  SplitItem,
  Stack,
  StackItem,
} from "@patternfly/react-core";
import { CheckIcon, PlusCircleIcon } from "@patternfly/react-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { useAdminClient } from "../../admin-client";
import { useRecentRealms } from "../../context/RecentRealms";
import { fetchAdminUI } from "../../context/auth/admin-ui-endpoint";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import NewRealmForm from "../../realm/add/NewRealmForm";

import "./realm-selector.css";

const MAX_RESULTS = 10;

type AddRealmProps = {
  onClick: () => void;
};

export const AddRealm = ({ onClick }: AddRealmProps) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button
        data-testid="add-realm"
        variant="plain"
        onClick={() => {
          onClick();
          setOpen(true);
        }}
      >
        <PlusCircleIcon /> {t("createRealm")}
      </Button>
      {open && <NewRealmForm onClose={() => setOpen(false)} />}
    </>
  );
};

type RealmTextProps = {
  name: string;
  displayName?: string;
  onClick?: () => void;
};

const RealmText = ({ name, displayName, onClick }: RealmTextProps) => {
  const { realm } = useRealm();
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <DropdownItem
      onClick={() => {
        if (name === realm) return;
        navigate(toDashboard({ realm: name }));
        onClick?.();
      }}
    >
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
      </Split>
    </DropdownItem>
  );
};

export type RealmNameRepresentation = {
  name: string;
  displayName?: string;
};

type RealmSelectorProps = {
  onViewAll: () => void;
};

export const RealmSelector = ({ onViewAll }: RealmSelectorProps) => {
  const { realm, realmRepresentation } = useRealm();
  const { adminClient } = useAdminClient();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [realms, setRealms] = useState<RealmNameRepresentation[]>([]);
  const { t } = useTranslation();
  const recentRealms = useRecentRealms();

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

  const recentRealmsList = useMemo(
    () => recentRealms.map((r) => r.name),
    [recentRealms],
  );

  const sortedRealms = useMemo(
    () =>
      realms
        .filter((r) => !recentRealmsList.includes(r.name))
        .sort((a, b) => {
          if (a.name === realm) return -1;
          if (b.name === realm) return 1;

          return a.name.localeCompare(b.name, whoAmI.getLocale());
        }),
    [recentRealmsList, realms],
  );

  const realmDisplayName = useMemo(
    () => realms.find((r) => r.name === realm)?.displayName,
    [realm, realms],
  );

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
      <DropdownGroup label={t("currentRealm")}>
        <DropdownList>
          <RealmText
            name={realm}
            displayName={realmRepresentation?.displayName}
          />
        </DropdownList>
      </DropdownGroup>
      <Divider component="li" />
      <DropdownGroup label={t("recentlyUsed")}>
        <DropdownList>
          {recentRealms
            .filter((r) => r.name !== realm)
            .map((realm) => (
              <RealmText
                {...realm}
                key={realm.name}
                onClick={() => setOpen(false)}
              />
            ))}
        </DropdownList>
      </DropdownGroup>
      <Divider component="li" />

      {realms.length <= MAX_RESULTS && (
        <DropdownList>
          {sortedRealms.map((realm) => (
            <RealmText
              {...realm}
              key={realm.name}
              onClick={() => setOpen(false)}
            />
          ))}
        </DropdownList>
      )}
      {realms.length > MAX_RESULTS && (
        <DropdownItem onClick={() => onViewAll()}>
          <Button variant="link">{t("viewAll")}</Button>
        </DropdownItem>
      )}
    </Dropdown>
  );
};
