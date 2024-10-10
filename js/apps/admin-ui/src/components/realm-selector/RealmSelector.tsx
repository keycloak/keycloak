import { NetworkError } from "@keycloak/keycloak-admin-client";
import { label, useFetch } from "@keycloak/keycloak-ui-shared";
import {
  Button,
  Divider,
  Dropdown,
  DropdownGroup,
  DropdownItem,
  DropdownList,
  Label,
  MenuToggle,
  SearchInput,
  Spinner,
  Split,
  SplitItem,
  Stack,
  StackItem,
} from "@patternfly/react-core";
import {
  AngleLeftIcon,
  AngleRightIcon,
  CheckIcon,
} from "@patternfly/react-icons";
import { debounce } from "lodash-es";
import { Fragment, useCallback, useMemo, useState } from "react";
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

type RealmNameRepresentation = {
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

  const [search, setSearch] = useState("");
  const [first, setFirst] = useState(0);

  const debounceFn = useCallback(
    debounce((value: string) => {
      setFirst(0);
      setSearch(value);
    }, 1000),
    [],
  );

  useFetch(
    async () => {
      try {
        return await fetchAdminUI<RealmNameRepresentation[]>(
          adminClient,
          "ui-ext/realms/names",
          { first: `${first}`, max: `${MAX_RESULTS + 1}`, search },
        );
      } catch (error) {
        if (error instanceof NetworkError && error.response.status < 500) {
          return [];
        }

        throw error;
      }
    },
    setRealms,
    [open, first, search],
  );

  const sortedRealms = useMemo(
    () => [
      ...(first === 0 && !search
        ? recentRealms.reduce((acc, name) => {
            const realm = realms.find((r) => r.name === name);
            if (realm) {
              acc.push(realm);
            }
            return acc;
          }, [] as RealmNameRepresentation[])
        : []),
      ...realms.filter((r) => !recentRealms.includes(r.name)),
    ],
    [recentRealms, realms, first, search],
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
      <DropdownList>
        {(realms.length > 5 || search || first !== 0) && (
          <>
            <DropdownGroup>
              <DropdownList>
                <SearchInput
                  value={search}
                  onChange={(_, value) => debounceFn(value)}
                  onClear={() => setSearch("")}
                />
              </DropdownList>
            </DropdownGroup>
            <Divider component="li" />
          </>
        )}
        {(realms.length !== 0
          ? [
              first !== 0 ? (
                <DropdownItem
                  onClick={(e) => {
                    e.stopPropagation();
                    setFirst(first - MAX_RESULTS);
                  }}
                >
                  <AngleLeftIcon /> {t("previous")}
                </DropdownItem>
              ) : (
                []
              ),
              ...sortedRealms.map((realm) => (
                <DropdownItem
                  key={realm.name}
                  onClick={() => {
                    navigate(toDashboard({ realm: realm.name }));
                    setOpen(false);
                    setSearch("");
                  }}
                >
                  <RealmText
                    {...realm}
                    showIsRecent={
                      realms.length > 5 && recentRealms.includes(realm.name)
                    }
                  />
                </DropdownItem>
              )),
              realms.length > MAX_RESULTS ? (
                <DropdownItem
                  onClick={(e) => {
                    e.stopPropagation();
                    setFirst(first + MAX_RESULTS);
                  }}
                >
                  <AngleRightIcon />
                  {t("next")}
                </DropdownItem>
              ) : (
                []
              ),
            ]
          : !search
            ? [
                <DropdownItem key="loader">
                  <Spinner size="sm" /> {t("loadingRealms")}
                </DropdownItem>,
              ]
            : [
                <DropdownItem key="no-results">
                  {t("noResultsFound")}
                </DropdownItem>,
              ]
        ).concat([
          <Fragment key="add-realm">
            {whoAmI.canCreateRealm() && (
              <>
                <Divider key="divider" />
                <DropdownItem key="add" component="div">
                  <AddRealm onClick={() => setOpen(false)} />
                </DropdownItem>
              </>
            )}
          </Fragment>,
        ])}
      </DropdownList>
    </Dropdown>
  );
};
