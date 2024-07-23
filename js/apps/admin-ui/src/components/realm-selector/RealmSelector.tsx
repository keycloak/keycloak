import { label } from "@keycloak/keycloak-ui-shared";
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
import { CheckIcon } from "@patternfly/react-icons";
import { Fragment, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";
import { useRealms } from "../../context/RealmsContext";
import { useRecentRealms } from "../../context/RecentRealms";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { toAddRealm } from "../../realm/routes/AddRealm";

import "./realm-selector.css";

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
    <Split className="keycloak__realm_selector__list-item-split">
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

export const RealmSelector = () => {
  const { realm } = useRealm();
  const { realms } = useRealms();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const { t } = useTranslation();
  const recentRealms = useRecentRealms();
  const navigate = useNavigate();

  const all = useMemo(
    () =>
      realms
        .map((realm) => {
          const used = recentRealms.some((name) => name === realm.name);
          return { realm, used };
        })
        .sort((r1, r2) => {
          if (r1.used == r2.used) return 0;
          if (r1.used) return -1;
          if (r2.used) return 1;
          return 0;
        }),
    [recentRealms, realm, realms],
  );

  const filteredItems = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    if (normalizedSearch.length === 0) {
      return all;
    }

    return search.trim() === ""
      ? all
      : all.filter(
          (r) =>
            r.realm.name.toLowerCase().includes(normalizedSearch) ||
            label(t, r.realm.displayName)
              ?.toLowerCase()
              .includes(normalizedSearch),
        );
  }, [search, all]);

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
          {label(t, realmDisplayName, realm)}
        </MenuToggle>
      )}
    >
      <DropdownList>
        {realms.length > 5 && (
          <>
            <DropdownGroup>
              <DropdownList>
                <SearchInput
                  value={search}
                  onChange={(_, value) => setSearch(value)}
                  onClear={() => setSearch("")}
                />
              </DropdownList>
            </DropdownGroup>
            <Divider component="li" />
          </>
        )}
        {(realms.length !== 0
          ? filteredItems.map((i) => (
              <DropdownItem
                key={i.realm.name}
                onClick={() => {
                  navigate(toDashboard({ realm: i.realm.name }));
                  setOpen(false);
                }}
              >
                <RealmText
                  {...i.realm}
                  showIsRecent={realms.length > 5 && i.used}
                />
              </DropdownItem>
            ))
          : [
              <DropdownItem key="loader">
                <Spinner size="sm" /> {t("loadingRealms")}
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
