import type EventRepresentation from "@keycloak/keycloak-admin-client/lib/defs/eventRepresentation";
import type EventType from "@keycloak/keycloak-admin-client/lib/defs/eventTypes";
import type { RealmEventsConfigRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/realmEventsConfigRepresentation";
import {
  KeycloakDataTable,
  KeycloakSelect,
  ListEmptyState,
  SelectVariant,
  TextControl,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import {
  ActionGroup,
  Button,
  Chip,
  ChipGroup,
  DatePicker,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Icon,
  SelectOption,
  Tooltip,
} from "@patternfly/react-core";
import { CheckCircleIcon, WarningTriangleIcon } from "@patternfly/react-icons";
import { cellWidth } from "@patternfly/react-table";
import { pickBy } from "lodash-es";
import { useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useAdminClient } from "../admin-client";
import DropdownPanel from "../components/dropdown-panel/DropdownPanel";
import { useRealm } from "../context/realm-context/RealmContext";
import { toUser } from "../user/routes/User";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../utils/useFormatDate";

import "./events.css";

type UserEventSearchForm = {
  client: string;
  dateFrom: string;
  dateTo: string;
  user: string;
  type: EventType[];
  ipAddress: string;
};

const StatusRow = (event: EventRepresentation) =>
  !event.error ? (
    <span>
      <Icon status="success">
        <CheckCircleIcon />
      </Icon>
      {event.type}
    </span>
  ) : (
    <Tooltip content={event.error}>
      <span>
        <Icon status="warning">
          <WarningTriangleIcon />
        </Icon>
        {event.type}
      </span>
    </Tooltip>
  );

const DetailCell = (event: EventRepresentation) => (
  <DescriptionList isHorizontal className="keycloak_eventsection_details">
    {event.details &&
      Object.entries(event.details).map(([key, value]) => (
        <DescriptionListGroup key={key}>
          <DescriptionListTerm>{key}</DescriptionListTerm>
          <DescriptionListDescription>{value}</DescriptionListDescription>
        </DescriptionListGroup>
      ))}
    {event.error && (
      <DescriptionListGroup key="error">
        <DescriptionListTerm>error</DescriptionListTerm>
        <DescriptionListDescription>{event.error}</DescriptionListDescription>
      </DescriptionListGroup>
    )}
  </DescriptionList>
);

const UserDetailLink = (event: EventRepresentation) => {
  const { t } = useTranslation();
  const { realm } = useRealm();

  return (
    <>
      {event.userId && (
        <Link
          key={`link-${event.time}-${event.type}`}
          to={toUser({
            realm,
            id: event.userId,
            tab: "settings",
          })}
        >
          {event.userId}
        </Link>
      )}
      {!event.userId && t("noUserDetails")}
    </>
  );
};

type UserEventsProps = {
  user?: string;
  client?: string;
};

export const UserEvents = ({ user, client }: UserEventsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
  const { realm } = useRealm();
  const formatDate = useFormatDate();
  const [key, setKey] = useState(0);
  const [searchDropdownOpen, setSearchDropdownOpen] = useState(false);
  const [selectOpen, setSelectOpen] = useState(false);
  const [events, setEvents] = useState<RealmEventsConfigRepresentation>();
  const [activeFilters, setActiveFilters] = useState<
    Partial<UserEventSearchForm>
  >({
    ...(user && { user }),
    ...(client && { client }),
  });

  const defaultValues: UserEventSearchForm = {
    client: client ? client : "",
    dateFrom: "",
    dateTo: "",
    user: user ? user : "",
    type: [],
    ipAddress: "",
  };

  const filterLabels: Record<keyof UserEventSearchForm, string> = {
    client: t("client"),
    dateFrom: t("dateFrom"),
    dateTo: t("dateTo"),
    user: t("userId"),
    type: t("eventType"),
    ipAddress: t("ipAddress"),
  };

  const form = useForm<UserEventSearchForm>({
    mode: "onChange",
    defaultValues,
  });

  const {
    getValues,
    reset,
    formState: { isDirty },
    control,
    handleSubmit,
  } = form;

  useFetch(
    () => adminClient.realms.getConfigEvents({ realm }),
    (events) => setEvents(events),
    [],
  );

  function loader(first?: number, max?: number) {
    return adminClient.realms.findEvents({
      // The admin client wants 'dateFrom' and 'dateTo' to be Date objects, however it cannot actually handle them so we need to cast to any.
      ...(activeFilters as any),
      realm,
      first,
      max,
    });
  }

  function onSubmit() {
    setSearchDropdownOpen(false);
    commitFilters();
  }

  function resetSearch() {
    reset();
    commitFilters();
  }

  function removeFilter(key: keyof UserEventSearchForm) {
    const formValues: UserEventSearchForm = { ...getValues() };
    delete formValues[key];

    reset({ ...defaultValues, ...formValues });
    commitFilters();
  }

  function removeFilterValue(
    key: keyof UserEventSearchForm,
    valueToRemove: EventType,
  ) {
    const formValues = getValues();
    const fieldValue = formValues[key];
    const newFieldValue = Array.isArray(fieldValue)
      ? fieldValue.filter((val) => val !== valueToRemove)
      : fieldValue;

    reset({ ...formValues, [key]: newFieldValue });
    commitFilters();
  }

  function commitFilters() {
    const newFilters: Partial<UserEventSearchForm> = pickBy(
      getValues(),
      (value) => value !== "" || (Array.isArray(value) && value.length > 0),
    );

    setActiveFilters(newFilters);
    setKey(key + 1);
  }

  const userEventSearchFormDisplay = () => {
    return (
      <FormProvider {...form}>
        <Flex
          direction={{ default: "column" }}
          spaceItems={{ default: "spaceItemsNone" }}
        >
          <FlexItem>
            <DropdownPanel
              buttonText={t("searchForUserEvent")}
              setSearchDropdownOpen={setSearchDropdownOpen}
              searchDropdownOpen={searchDropdownOpen}
              marginRight="2.5rem"
              width="15vw"
            >
              <Form
                data-testid="searchForm"
                className="keycloak__events_search__form"
                onSubmit={handleSubmit(onSubmit)}
                isHorizontal
              >
                <TextControl
                  name="user"
                  label={t("userId")}
                  data-testid="userId-searchField"
                  isDisabled={!!user}
                />
                <FormGroup
                  label={t("eventType")}
                  fieldId="kc-eventType"
                  className="keycloak__events_search__form_label"
                >
                  <Controller
                    name="type"
                    control={control}
                    render={({ field }) => (
                      <KeycloakSelect
                        className="keycloak__events_search__type_select"
                        data-testid="event-type-searchField"
                        chipGroupProps={{
                          numChips: 1,
                          expandedText: t("hide"),
                          collapsedText: t("showRemaining"),
                        }}
                        variant={SelectVariant.typeaheadMulti}
                        typeAheadAriaLabel="Select"
                        onToggle={(isOpen) => setSelectOpen(isOpen)}
                        selections={field.value}
                        onSelect={(selectedValue) => {
                          const option = selectedValue.toString() as EventType;
                          const changedValue = field.value.includes(option)
                            ? field.value.filter((item) => item !== option)
                            : [...field.value, option];

                          field.onChange(changedValue);
                        }}
                        onClear={() => {
                          field.onChange([]);
                        }}
                        isOpen={selectOpen}
                        aria-labelledby={"eventType"}
                        chipGroupComponent={
                          <ChipGroup>
                            {field.value.map((chip) => (
                              <Chip
                                key={chip}
                                onClick={(event) => {
                                  event.stopPropagation();
                                  field.onChange(
                                    field.value.filter((val) => val !== chip),
                                  );
                                }}
                              >
                                {t(`eventTypes.${chip}.name`)}
                              </Chip>
                            ))}
                          </ChipGroup>
                        }
                      >
                        {events?.enabledEventTypes?.map((option) => (
                          <SelectOption key={option} value={option}>
                            {t(`eventTypes.${option}.name`)}
                          </SelectOption>
                        ))}
                      </KeycloakSelect>
                    )}
                  />
                </FormGroup>
                <TextControl
                  name="client"
                  label={t("client")}
                  data-testid="client-searchField"
                  isDisabled={!!client}
                />
                <FormGroup
                  label={t("dateFrom")}
                  fieldId="kc-dateFrom"
                  className="keycloak__events_search__form_label"
                >
                  <Controller
                    name="dateFrom"
                    control={control}
                    render={({ field }) => (
                      <DatePicker
                        className="pf-v5-u-w-100"
                        value={field.value}
                        onChange={(_, value) => field.onChange(value)}
                        inputProps={{ id: "kc-dateFrom" }}
                      />
                    )}
                  />
                </FormGroup>
                <FormGroup
                  label={t("dateTo")}
                  fieldId="kc-dateTo"
                  className="keycloak__events_search__form_label"
                >
                  <Controller
                    name="dateTo"
                    control={control}
                    render={({ field }) => (
                      <DatePicker
                        className="pf-v5-u-w-100"
                        value={field.value}
                        onChange={(_, value) => field.onChange(value)}
                        inputProps={{ id: "kc-dateTo" }}
                      />
                    )}
                  />
                </FormGroup>
                <TextControl
                  name="ipAddress"
                  label={t("ipAddress")}
                  data-testid="ipAddress-searchField"
                />
                <ActionGroup>
                  <Button
                    data-testid="search-events-btn"
                    variant="primary"
                    type="submit"
                    isDisabled={!isDirty}
                  >
                    {t("searchUserEventsBtn")}
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={resetSearch}
                    isDisabled={!isDirty}
                  >
                    {t("resetBtn")}
                  </Button>
                </ActionGroup>
              </Form>
            </DropdownPanel>
          </FlexItem>
          <FlexItem>
            {Object.entries(activeFilters).length > 0 && (
              <div className="keycloak__searchChips pf-v5-u-ml-md">
                {Object.entries(activeFilters).map((filter) => {
                  const [key, value] = filter as [
                    keyof UserEventSearchForm,
                    string | EventType[],
                  ];

                  const disableClose =
                    (key === "user" && !!user) ||
                    (key === "client" && !!client);

                  return (
                    <ChipGroup
                      className="pf-v5-u-mt-md pf-v5-u-mr-md"
                      key={key}
                      categoryName={filterLabels[key]}
                      isClosable={!disableClose}
                      onClick={() => removeFilter(key)}
                    >
                      {typeof value === "string" ? (
                        <Chip isReadOnly>{value}</Chip>
                      ) : (
                        value.map((entry) => (
                          <Chip
                            key={entry}
                            onClick={() => removeFilterValue(key, entry)}
                          >
                            {t(`eventTypes.${entry}.name`)}
                          </Chip>
                        ))
                      )}
                    </ChipGroup>
                  );
                })}
              </div>
            )}
          </FlexItem>
        </Flex>
      </FormProvider>
    );
  };

  return (
    <div className="keycloak__events_table">
      <KeycloakDataTable
        key={key}
        loader={loader}
        detailColumns={[
          {
            name: "details",
            enabled: (event) => event.details !== undefined,
            cellRenderer: DetailCell,
          },
        ]}
        isPaginated
        ariaLabelKey="titleEvents"
        toolbarItem={userEventSearchFormDisplay()}
        columns={[
          {
            name: "time",
            displayKey: "time",
            cellRenderer: (row) =>
              formatDate(new Date(row.time!), FORMAT_DATE_AND_TIME),
          },
          {
            name: "userId",
            displayKey: "user",
            cellRenderer: UserDetailLink,
          },
          {
            name: "type",
            displayKey: "eventType",
            cellRenderer: StatusRow,
          },
          {
            name: "ipAddress",
            displayKey: "ipAddress",
            transforms: [cellWidth(10)],
          },
          {
            name: "clientId",
            displayKey: "client",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyUserEvents")}
            instructions={t("emptyUserEventsInstructions")}
          />
        }
        isSearching={Object.keys(activeFilters).length > 0}
      />
    </div>
  );
};
