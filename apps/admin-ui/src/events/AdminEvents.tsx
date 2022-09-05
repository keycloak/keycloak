import {
  ActionGroup,
  Button,
  Chip,
  ChipGroup,
  DatePicker,
  Dropdown,
  DropdownToggle,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Modal,
  ModalVariant,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import {
  cellWidth,
  Table,
  TableBody,
  TableHeader,
  TableVariant,
} from "@patternfly/react-table";
import { CodeEditor, Language } from "@patternfly/react-code-editor";
import type AdminEventRepresentation from "@keycloak/keycloak-admin-client/lib/defs/adminEventRepresentation";
import { FunctionComponent, useMemo, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { pickBy } from "lodash-es";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../components/table-toolbar/KeycloakDataTable";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { useAdminClient } from "../context/auth/AdminClient";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../utils/useFormatDate";
import { prettyPrintJSON } from "../util";
import { CellResourceLinkRenderer } from "./ResourceLinks";

import "./events.css";

type DisplayDialogProps = {
  titleKey: string;
  onClose: () => void;
};

type AdminEventSearchForm = {
  resourceTypes: string[];
  operationTypes: string[];
  resourcePath: string;
  dateFrom: string;
  dateTo: string;
  authClient: string;
  authUser: string;
  authRealm: string;
  authIpAddress: string;
};

const defaultValues: AdminEventSearchForm = {
  resourceTypes: [],
  operationTypes: [],
  resourcePath: "",
  dateFrom: "",
  dateTo: "",
  authClient: "",
  authUser: "",
  authRealm: "",
  authIpAddress: "",
};

const DisplayDialog: FunctionComponent<DisplayDialogProps> = ({
  titleKey,
  onClose,
  children,
}) => {
  const { t } = useTranslation("events");
  return (
    <Modal
      variant={ModalVariant.medium}
      title={t(titleKey)}
      isOpen={true}
      onClose={onClose}
    >
      {children}
    </Modal>
  );
};

export const AdminEvents = () => {
  const { t } = useTranslation("events");
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();
  const serverInfo = useServerInfo();
  const formatDate = useFormatDate();
  const resourceTypes = serverInfo.enums?.["resourceType"];
  const operationTypes = serverInfo.enums?.["operationType"];

  const [key, setKey] = useState(0);
  const [searchDropdownOpen, setSearchDropdownOpen] = useState(false);
  const [selectResourceTypesOpen, setSelectResourceTypesOpen] = useState(false);
  const [selectOperationTypesOpen, setSelectOperationTypesOpen] =
    useState(false);
  const [activeFilters, setActiveFilters] = useState<
    Partial<AdminEventSearchForm>
  >({});

  const [authEvent, setAuthEvent] = useState<AdminEventRepresentation>();
  const [representationEvent, setRepresentationEvent] =
    useState<AdminEventRepresentation>();

  const filterLabels: Record<keyof AdminEventSearchForm, string> = {
    resourceTypes: t("resourceTypes"),
    operationTypes: t("operationTypes"),
    resourcePath: t("resourcePath"),
    dateFrom: t("dateFrom"),
    dateTo: t("dateTo"),
    authClient: t("client"),
    authUser: t("userId"),
    authRealm: t("realm"),
    authIpAddress: t("ipAddress"),
  };

  const {
    getValues,
    register,
    reset,
    formState: { isDirty },
    control,
  } = useForm<AdminEventSearchForm>({
    shouldUnregister: false,
    mode: "onChange",
    defaultValues,
  });

  function loader(first?: number, max?: number) {
    return adminClient.realms.findAdminEvents({
      // The admin client wants 'dateFrom' and 'dateTo' to be Date objects, however it cannot actually handle them so we need to cast to any.
      ...(activeFilters as any),
      realm,
      first,
      max,
    });
  }

  function submitSearch() {
    setSearchDropdownOpen(false);
    commitFilters();
  }

  function resetSearch() {
    reset();
    commitFilters();
  }

  function removeFilter(key: keyof AdminEventSearchForm) {
    const formValues: AdminEventSearchForm = { ...getValues() };
    delete formValues[key];

    reset({ ...defaultValues, ...formValues });
    commitFilters();
  }

  function removeFilterValue(
    key: keyof AdminEventSearchForm,
    valueToRemove: string
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
    const newFilters: Partial<AdminEventSearchForm> = pickBy(
      getValues(),
      (value) => value !== "" || (Array.isArray(value) && value.length > 0)
    );

    setActiveFilters(newFilters);
    setKey(key + 1);
  }

  function refresh() {
    commitFilters();
  }

  const adminEventSearchFormDisplay = () => {
    return (
      <Flex
        direction={{ default: "column" }}
        spaceItems={{ default: "spaceItemsNone" }}
      >
        <FlexItem>
          <Dropdown
            id="admin-events-search-select"
            data-testid="AdminEventsSearchSelector"
            className="pf-u-ml-md"
            toggle={
              <DropdownToggle
                data-testid="adminEventsSearchSelectorToggle"
                onToggle={(isOpen) => setSearchDropdownOpen(isOpen)}
                className="keycloak__events_search_selector_dropdown__toggle"
              >
                {t("searchForAdminEvent")}
              </DropdownToggle>
            }
            isOpen={searchDropdownOpen}
          >
            <Form
              isHorizontal
              className="keycloak__events_search__form"
              data-testid="searchForm"
            >
              <FormGroup
                label={t("resourceTypes")}
                fieldId="kc-resourceTypes"
                className="keycloak__events_search__form_label"
              >
                <Controller
                  name="resourceTypes"
                  control={control}
                  render={({
                    onChange,
                    value,
                  }: {
                    onChange: (newValue: string[]) => void;
                    value: string[];
                  }) => (
                    <Select
                      className="keycloak__events_search__type_select"
                      name="resourceTypes"
                      data-testid="resource-types-searchField"
                      chipGroupProps={{
                        numChips: 1,
                        expandedText: t("common:hide"),
                        collapsedText: t("common:showRemaining"),
                      }}
                      variant={SelectVariant.typeaheadMulti}
                      typeAheadAriaLabel="Select"
                      onToggle={(isOpen) => setSelectResourceTypesOpen(isOpen)}
                      selections={value}
                      onSelect={(_, selectedValue) => {
                        const option = selectedValue.toString();
                        const changedValue = value.includes(option)
                          ? value.filter((item) => item !== option)
                          : [...value, option];

                        onChange(changedValue);
                      }}
                      onClear={(resource) => {
                        resource.stopPropagation();
                        onChange([]);
                      }}
                      isOpen={selectResourceTypesOpen}
                      aria-labelledby={"resourceTypes"}
                      chipGroupComponent={
                        <ChipGroup>
                          {value.map((chip) => (
                            <Chip
                              key={chip}
                              onClick={(resource) => {
                                resource.stopPropagation();
                                onChange(value.filter((val) => val !== chip));
                              }}
                            >
                              {chip}
                            </Chip>
                          ))}
                        </ChipGroup>
                      }
                    >
                      {resourceTypes?.map((option) => (
                        <SelectOption key={option} value={option} />
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("operationTypes")}
                fieldId="kc-operationTypes"
                className="keycloak__events_search__form_label"
              >
                <Controller
                  name="operationTypes"
                  control={control}
                  render={({
                    onChange,
                    value,
                  }: {
                    onChange: (newValue: string[]) => void;
                    value: string[];
                  }) => (
                    <Select
                      className="keycloak__events_search__type_select"
                      name="operationTypes"
                      data-testid="operation-types-searchField"
                      chipGroupProps={{
                        numChips: 1,
                        expandedText: t("common:hide"),
                        collapsedText: t("common:showRemaining"),
                      }}
                      variant={SelectVariant.typeaheadMulti}
                      typeAheadAriaLabel="Select"
                      onToggle={(isOpen) => setSelectOperationTypesOpen(isOpen)}
                      selections={value}
                      onSelect={(_, selectedValue) => {
                        const option = selectedValue.toString();
                        const changedValue = value.includes(option)
                          ? value.filter((item) => item !== option)
                          : [...value, option];

                        onChange(changedValue);
                      }}
                      onClear={(operation) => {
                        operation.stopPropagation();
                        onChange([]);
                      }}
                      isOpen={selectOperationTypesOpen}
                      aria-labelledby={"operationTypes"}
                      chipGroupComponent={
                        <ChipGroup>
                          {value.map((chip) => (
                            <Chip
                              key={chip}
                              onClick={(operation) => {
                                operation.stopPropagation();
                                onChange(value.filter((val) => val !== chip));
                              }}
                            >
                              {chip}
                            </Chip>
                          ))}
                        </ChipGroup>
                      }
                    >
                      {operationTypes?.map((option) => (
                        <SelectOption key={option} value={option} />
                      ))}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup
                label={t("resourcePath")}
                fieldId="kc-resourcePath"
                className="keycloak__events_search__form_label"
              >
                <KeycloakTextInput
                  ref={register()}
                  type="text"
                  id="kc-resourcePath"
                  name="resourcePath"
                  data-testid="resourcePath-searchField"
                />
              </FormGroup>
              <FormGroup
                label={t("realm")}
                fieldId="kc-realm"
                className="keycloak__events_search__form_label"
              >
                <KeycloakTextInput
                  ref={register()}
                  type="text"
                  id="kc-realm"
                  name="authRealm"
                  data-testid="realm-searchField"
                />
              </FormGroup>
              <FormGroup
                label={t("client")}
                fieldId="kc-client"
                className="keycloak__events_search__form_label"
              >
                <KeycloakTextInput
                  ref={register()}
                  type="text"
                  id="kc-client"
                  name="authClient"
                  data-testid="client-searchField"
                />
              </FormGroup>
              <FormGroup
                label={t("user")}
                fieldId="kc-user"
                className="keycloak__events_search__form_label"
              >
                <KeycloakTextInput
                  ref={register()}
                  type="text"
                  id="kc-user"
                  name="authUser"
                  data-testid="user-searchField"
                />
              </FormGroup>
              <FormGroup
                label={t("ipAddress")}
                fieldId="kc-ipAddress"
                className="keycloak__events_search__form_label"
              >
                <KeycloakTextInput
                  ref={register()}
                  type="text"
                  id="kc-ipAddress"
                  name="authIpAddress"
                  data-testid="ipAddress-searchField"
                />
              </FormGroup>
              <FormGroup
                label={t("dateFrom")}
                fieldId="kc-dateFrom"
                className="keycloak__events_search__form_label"
              >
                <Controller
                  name="dateFrom"
                  control={control}
                  render={({ onChange, value }) => (
                    <DatePicker
                      className="pf-u-w-100"
                      value={value}
                      onChange={(value) => onChange(value)}
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
                  render={({ onChange, value }) => (
                    <DatePicker
                      className="pf-u-w-100"
                      value={value}
                      onChange={(value) => onChange(value)}
                      inputProps={{ id: "kc-dateTo" }}
                    />
                  )}
                />
              </FormGroup>
              <ActionGroup>
                <Button
                  variant={"primary"}
                  onClick={submitSearch}
                  data-testid="search-events-btn"
                  isDisabled={!isDirty}
                >
                  {t("searchAdminEventsBtn")}
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
          </Dropdown>
          <Button
            className="pf-u-ml-md"
            onClick={refresh}
            data-testid="refresh-btn"
          >
            {t("refresh")}
          </Button>
        </FlexItem>
        <FlexItem>
          {Object.entries(activeFilters).length > 0 && (
            <div className="keycloak__searchChips pf-u-ml-md">
              {Object.entries(activeFilters).map((filter) => {
                const [key, value] = filter as [
                  keyof AdminEventSearchForm,
                  string | string[]
                ];

                return (
                  <ChipGroup
                    className="pf-u-mt-md pf-u-mr-md"
                    key={key}
                    categoryName={filterLabels[key]}
                    isClosable
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
                          {entry}
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
    );
  };

  const rows = [
    [t("realm"), authEvent?.authDetails?.realmId],
    [t("client"), authEvent?.authDetails?.clientId],
    [t("user"), authEvent?.authDetails?.userId],
    [t("ipAddress"), authEvent?.authDetails?.ipAddress],
  ];

  const code = useMemo(
    () =>
      representationEvent?.representation
        ? prettyPrintJSON(JSON.parse(representationEvent.representation))
        : "",
    [representationEvent?.representation]
  );

  return (
    <>
      {authEvent && (
        <DisplayDialog titleKey="auth" onClose={() => setAuthEvent(undefined)}>
          <Table
            aria-label="authData"
            data-testid="auth-dialog"
            variant={TableVariant.compact}
            cells={[t("attribute"), t("value")]}
            rows={rows}
          >
            <TableHeader />
            <TableBody />
          </Table>
        </DisplayDialog>
      )}
      {representationEvent && (
        <DisplayDialog
          titleKey="representation"
          data-testid="representation-dialog"
          onClose={() => setRepresentationEvent(undefined)}
        >
          <CodeEditor
            isLineNumbersVisible
            isReadOnly
            code={code}
            language={Language.json}
            height="8rem"
          />
        </DisplayDialog>
      )}
      <KeycloakDataTable
        key={key}
        loader={loader}
        isPaginated
        ariaLabelKey="events:adminEvents"
        toolbarItem={adminEventSearchFormDisplay()}
        actions={[
          {
            title: t("auth"),
            onRowClick: (event) => setAuthEvent(event),
          },
          {
            title: t("representation"),
            onRowClick: (event) => setRepresentationEvent(event),
          },
        ]}
        columns={[
          {
            name: "time",
            displayKey: "events:time",
            cellRenderer: (row) =>
              formatDate(new Date(row.time!), FORMAT_DATE_AND_TIME),
          },
          {
            name: "resourcePath",
            displayKey: "events:resourcePath",
            cellRenderer: CellResourceLinkRenderer,
          },
          {
            name: "resourceType",
            displayKey: "events:resourceType",
          },
          {
            name: "operationType",
            displayKey: "events:operationType",
            transforms: [cellWidth(10)],
          },
          {
            name: "",
            displayKey: "events:user",
            cellRenderer: (event) => event.authDetails?.userId,
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyEvents")}
            instructions={t("emptyEventsInstructions")}
          />
        }
        isSearching={Object.keys(activeFilters).length > 0}
      />
    </>
  );
};
