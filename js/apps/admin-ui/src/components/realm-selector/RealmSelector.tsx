import {
  Button,
  ContextSelector,
  ContextSelectorItem,
  ContextSelectorItemProps,
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
import { Fragment, useState, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Link, To, useHref } from "react-router-dom";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useRealms } from "../../context/RealmsContext";
import { useRecentRealms } from "../../context/RecentRealms";
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

// We need to make all these props partial because of a bug in PatternFly.
// See: https://github.com/patternfly/patternfly-react/pull/8670
// TODO: Remove this partial when a fix has been released.
type ContextSelectorItemLinkProps = Partial<
  Omit<ContextSelectorItemProps, "href">
> & {
  to: To;
};

const ContextSelectorItemLink = ({
  to,
  ...props
}: ContextSelectorItemLinkProps) => {
  const href = useHref(to);
  return <ContextSelectorItem {...props} href={href} />;
};

export const RealmSelector = () => {
  const { realm } = useRealm();
  const { realms, refresh } = useRealms();
  const { whoAmI } = useWhoAmI();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const { t } = useTranslation();
  const recentRealms = useRecentRealms();

  const all = useMemo(
    () =>
      recentRealms
        .filter((r) => r !== realm)
        .map((name) => {
          return { name, used: true };
        })
        .concat(
          realms
            .filter((name) => !recentRealms.includes(name) || name === realm)
            .map((name) => ({ name, used: false })),
        ),
    [recentRealms, realm, realms],
  );

  const filteredItems = useMemo(
    () =>
      search.trim() === ""
        ? all
        : all.filter((r) =>
            r.name.toLowerCase().includes(search.toLowerCase()),
          ),
    [search, all],
  );

  return realms.length > 5 ? (
    <ContextSelector
      data-testid="realmSelector"
      toggleText={realm}
      isOpen={open}
      screenReaderLabel={realm}
      onToggle={() => setOpen(!open)}
      searchInputValue={search}
      onSearchInputChange={(value) => setSearch(value)}
      className="keycloak__realm_selector__context_selector"
      footer={
        whoAmI.canCreateRealm() && (
          <ContextSelectorItem key="add">
            <AddRealm onClick={() => setOpen(false)} />
          </ContextSelectorItem>
        )
      }
    >
      {filteredItems.map((item) => (
        <ContextSelectorItemLink
          key={item.name}
          to={toDashboard({ realm: item.name })}
          onClick={() => setOpen(false)}
        >
          <RealmText value={item.name} />{" "}
          {item.used && <Label>{t("recent")}</Label>}
        </ContextSelectorItemLink>
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
      dropdownItems={(realms.length !== 0
        ? realms.map((name) => (
            <DropdownItem
              key={name}
              component={
                <Link
                  to={toDashboard({ realm: name })}
                  onClick={() => setOpen(false)}
                >
                  <RealmText value={name} />
                </Link>
              }
            />
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
    />
  );
};
