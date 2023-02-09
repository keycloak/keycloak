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
import { Fragment, ReactElement, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation, useNavigate } from "react-router-dom";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useRealms } from "../../context/RealmsContext";
import { useRecentRealms } from "../../context/RecentRealms";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { toDashboard } from "../../dashboard/routes/Dashboard";
import { toAddRealm } from "../../realm/routes/AddRealm";
import { useUpdateEffect } from "../../utils/useUpdateEffect";

import "./realm-selector.css";

const AddRealm = () => {
  const { realm } = useRealm();
  const { t } = useTranslation("common");

  return (
    <Button
      data-testid="add-realm"
      component={(props) => <Link {...props} to={toAddRealm({ realm })} />}
      isBlock
    >
      {t("createRealm")}
    </Button>
  );
};

type RealmTextProps = {
  value: string;
};

const RealmText = ({ value }: RealmTextProps) => {
  const { realm } = useRealm();

  return (
    <Split className="keycloak__realm_selector__list-item-split">
      <SplitItem isFilled>{value}</SplitItem>
      <SplitItem>{value === realm && <CheckIcon />}</SplitItem>
    </Split>
  );
};

export const RealmSelector = () => {
  const { realm } = useRealm();
  const { realms, refresh } = useRealms();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation("common");
  const recentRealms = useRecentRealms();

  const all = recentRealms
    .filter((r) => r !== realm)
    .map((name) => {
      return { name, used: true };
    })
    .concat(
      realms
        .filter((r) => !recentRealms.includes(r.realm!) || r.realm === realm)
        .map((r) => {
          return { name: r.realm!, used: false };
        })
    );

  const filteredItems =
    search === ""
      ? undefined
      : all.filter((r) => r.name.toLowerCase().includes(search.toLowerCase()));

  useUpdateEffect(() => setOpen(false), [location]);

  const dropdownItems =
    realms.length !== 0
      ? realms.map((r) => (
          <DropdownItem
            key={`realm-dropdown-item-${r.realm}`}
            component={
              <Link to={toDashboard({ realm: r.realm! })}>
                <RealmText value={r.realm!} />
              </Link>
            }
          />
        ))
      : [
          <DropdownItem key="load">
            Loading <Spinner size="sm" />
          </DropdownItem>,
        ];

  return realms.length > 5 ? (
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
          navigate(toDashboard({ realm: value }));
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
  ) : (
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
      dropdownItems={[
        ...dropdownItems,
        <Fragment key="add-realm">
          {whoAmI.canCreateRealm() && (
            <>
              <Divider key="divider" />
              <DropdownItem key="add">
                <AddRealm />
              </DropdownItem>
            </>
          )}
        </Fragment>,
      ]}
    />
  );
};
