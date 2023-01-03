import {
  Button,
  ContextSelector,
  ContextSelectorItem,
  Divider,
  Dropdown,
  DropdownItem,
  DropdownToggle,
  Label,
  Spinner,
  Split,
  SplitItem,
} from "@patternfly/react-core";
import { CheckIcon } from "@patternfly/react-icons";
import { Fragment, ReactElement, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom-v5-compat";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useRealms } from "../../context/RealmsContext";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { toAddRealm } from "../../realm/routes/AddRealm";
import { RecentUsed } from "./recent-used";

import "./realm-selector.css";

export const RealmSelector = () => {
  const { realm } = useRealm();
  const { realms, refresh } = useRealms();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const navigate = useNavigate();
  const { t } = useTranslation("common");
  const recentUsed = new RecentUsed();
  const all = recentUsed.used
    .filter((r) => r !== realm)
    .map((name) => {
      return { name, used: true };
    })
    .concat(
      realms
        .filter((r) => !recentUsed.used.includes(r.realm!) || r.realm === realm)
        .map((r) => {
          return { name: r.realm!, used: false };
        })
    );

  const filteredItems = useMemo(() => {
    if (search === "") {
      return undefined;
    }

    return all.filter((r) =>
      r.name.toLowerCase().includes(search.toLowerCase())
    );
  }, [search, all]);

  const RealmText = ({ value }: { value: string }) => (
    <Split className="keycloak__realm_selector__list-item-split">
      <SplitItem isFilled>{value}</SplitItem>
      <SplitItem>{value === realm && <CheckIcon />}</SplitItem>
    </Split>
  );

  const AddRealm = () => (
    <Button
      data-testid="add-realm"
      component="div"
      isBlock
      onClick={() => {
        navigate(toAddRealm({ realm }));
        setOpen(!open);
      }}
    >
      {t("createRealm")}
    </Button>
  );

  const selectRealm = (realm: string) => {
    setOpen(!open);
    navigate(toDashboard({ realm }));
  };

  const dropdownItems =
    realms.length !== 0
      ? realms.map((r) => (
          <DropdownItem
            key={`realm-dropdown-item-${r.realm}`}
            onClick={() => {
              selectRealm(r.realm!);
            }}
          >
            <RealmText value={r.realm!} />
          </DropdownItem>
        ))
      : [
          <DropdownItem key="load">
            Loading <Spinner size="sm" />
          </DropdownItem>,
        ];

  const addRealmComponent = (
    <Fragment key="Add Realm">
      {whoAmI.canCreateRealm() && (
        <>
          <Divider key="divider" />
          <DropdownItem key="add">
            <AddRealm />
          </DropdownItem>
        </>
      )}
    </Fragment>
  );

  return (
    <>
      {realms.length > 5 && (
        <ContextSelector
          data-testid="realmSelector"
          toggleText={realm}
          isOpen={open}
          screenReaderLabel={realm}
          onToggle={() => setOpen(!open)}
          onSelect={(_, r) => {
            let element: ReactElement;
            if (Array.isArray(r)) {
              element = (r as ReactElement[])[0];
            } else {
              element = r as ReactElement;
            }
            const value = element.props.value;
            if (value) {
              selectRealm(value);
            }
          }}
          searchInputValue={search}
          onSearchInputChange={(value) => setSearch(value)}
          className="keycloak__realm_selector__context_selector"
          footer={
            whoAmI.canCreateRealm() && (
              <ContextSelectorItem key="add">
                <AddRealm />
              </ContextSelectorItem>
            )
          }
        >
          {(filteredItems || all).map((item) => (
            <ContextSelectorItem key={item.name}>
              <RealmText value={item.name} />{" "}
              {item.used && <Label>{t("recent")}</Label>}
            </ContextSelectorItem>
          ))}
        </ContextSelector>
      )}
      {realms.length <= 5 && (
        <Dropdown
          id="realm-select"
          data-testid="realmSelector"
          className="keycloak__realm_selector__dropdown"
          isOpen={open}
          toggle={
            <DropdownToggle
              data-testid="realmSelectorToggle"
              onToggle={() => {
                if (realms.length === 0) refresh();
                setOpen(!open);
              }}
              className="keycloak__realm_selector_dropdown__toggle"
            >
              {realm}
            </DropdownToggle>
          }
          dropdownItems={[...dropdownItems, addRealmComponent]}
        />
      )}
    </>
  );
};
