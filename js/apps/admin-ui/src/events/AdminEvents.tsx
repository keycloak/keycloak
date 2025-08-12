import type AdminEventRepresentation from "@keycloak/keycloak-admin-client/lib/defs/adminEventRepresentation";
import {
  Action,
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
  Modal,
  ModalVariant,
  SelectOption,
} from "@patternfly/react-core";
import {
  Table,
  TableVariant,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
  cellWidth,
} from "@patternfly/react-table";
import { pickBy } from "lodash-es";
import { PropsWithChildren, useMemo, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useAdminClient } from "../admin-client";
import { EventsBanners } from "../Banners";
import DropdownPanel from "../components/dropdown-panel/DropdownPanel";
import CodeEditor from "../components/form/CodeEditor";
import { useRealm } from "../context/realm-context/RealmContext";
import { useServerInfo } from "../context/server-info/ServerInfoProvider";
import { prettyPrintJSON } from "../util";
import useFormatDate, { FORMAT_DATE_AND_TIME } from "../utils/useFormatDate";
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

const DisplayDialog = ({
  titleKey,
  onClose,
  children,
}: PropsWithChildren<DisplayDialogProps>) => {
  const { t } = useTranslation();
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

const DetailCell = (event: AdminEventRepresentation) => (
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

type AdminEventsProps = {
  resourcePath?: string;
};

export const AdminEvents = ({ resourcePath }: AdminEventsProps) => {
  const { adminClient } = useAdminClient();

  const { t } = useTranslation();
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

  const defaultValues: AdminEventSearchForm = {
    resourceTypes: [],
    operationTypes: [],
    resourcePath: resourcePath ? resourcePath : "",
    dateFrom: "",
    dateTo: "",
    authClient: "",
    authUser: "",
    authRealm: "",
    authIpAddress: "",
  };

  const [authEvent, setAuthEvent] = useState<AdminEventRepresentation>();
  const [adminEventsEnabled, setAdminEventsEnabled] = useState<boolean>();
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

  const form = useForm<AdminEventSearchForm>({
    mode: "onChange",
    defaultValues,
  });
  const {
    getValues,
    reset,
    formState: { isDirty },
    control,
  } = form;

  useFetch(
    () => adminClient.realms.getConfigEvents({ realm }),
    (events) => {
      setAdminEventsEnabled(events?.adminEventsEnabled!);
    },
    [],
  );

  function loader(first?: number, max?: number) {
    return adminClient.realms.findAdminEvents({
      resourcePath,
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
    valueToRemove: string,
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
      (value) => value !== "" || (Array.isArray(value) && value.length > 0),
    );

    if (resourcePath) {
      delete newFilters.resourcePath;
    }

    setActiveFilters(newFilters);
    setKey(key + 1);
  }

  const code = useMemo(
    () =>
      representationEvent?.representation
        ? prettyPrintJSON(JSON.parse(representationEvent.representation))
        : "",
    [representationEvent?.representation],
  );

  return (
    <>
      {authEvent && (
        <DisplayDialog titleKey="auth" onClose={() => setAuthEvent(undefined)}>
          <Table
            aria-label="authData"
            data-testid="auth-dialog"
            variant={TableVariant.compact}
          >
            <Thead>
              <Tr>
                <Th>{t("attribute")}</Th>
                <Th>{t("value")}</Th>
              </Tr>
            </Thead>
            <Tbody>
              <Tr>
                <Td>{t("realm")}</Td>
                <Td>{authEvent.authDetails?.realmId}</Td>
              </Tr>
              <Tr>
                <Td>{t("client")}</Td>
                <Td>{authEvent.authDetails?.clientId}</Td>
              </Tr>
              <Tr>
                <Td>{t("user")}</Td>
                <Td>{authEvent.authDetails?.userId}</Td>
              </Tr>
              <Tr>
                <Td>{t("ipAddress")}</Td>
                <Td>{authEvent.authDetails?.ipAddress}</Td>
              </Tr>
            </Tbody>
          </Table>
        </DisplayDialog>
      )}
      {representationEvent && (
        <DisplayDialog
          titleKey="representation"
          data-testid="representation-dialog"
          onClose={() => setRepresentationEvent(undefined)}
        >
          <CodeEditor readOnly value={code} language="json" />
        </DisplayDialog>
      )}
      {!adminEventsEnabled && <EventsBanners type="adminEvents" />}
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
        ariaLabelKey="adminEvents"
        toolbarItem={
          <FormProvider {...form}>
            <Flex
              direction={{ default: "column" }}
              spaceItems={{ default: "spaceItemsNone" }}
            >
              <FlexItem>
                <DropdownPanel
                  buttonText={t("searchForAdminEvent")}
                  setSearchDropdownOpen={setSearchDropdownOpen}
                  searchDropdownOpen={searchDropdownOpen}
                  marginRight="2.5rem"
                  width="15vw"
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
                        render={({ field }) => (
                          <KeycloakSelect
                            className="keycloak__events_search__type_select"
                            data-testid="resource-types-searchField"
                            chipGroupProps={{
                              numChips: 1,
                              expandedText: t("hide"),
                              collapsedText: t("showRemaining"),
                            }}
                            variant={SelectVariant.typeaheadMulti}
                            typeAheadAriaLabel="Select"
                            onToggle={(isOpen) =>
                              setSelectResourceTypesOpen(isOpen)
                            }
                            selections={field.value}
                            onSelect={(selectedValue) => {
                              const option = selectedValue.toString();
                              const changedValue = field.value.includes(option)
                                ? field.value.filter(
                                    (item: string) => item !== option,
                                  )
                                : [...field.value, option];

                              field.onChange(changedValue);
                            }}
                            onClear={() => {
                              field.onChange([]);
                            }}
                            isOpen={selectResourceTypesOpen}
                            aria-labelledby={"resourceTypes"}
                            chipGroupComponent={
                              <ChipGroup>
                                {field.value.map((chip: string) => (
                                  <Chip
                                    key={chip}
                                    onClick={(resource) => {
                                      resource.stopPropagation();
                                      field.onChange(
                                        field.value.filter(
                                          (val: string) => val !== chip,
                                        ),
                                      );
                                    }}
                                  >
                                    {chip}
                                  </Chip>
                                ))}
                              </ChipGroup>
                            }
                          >
                            {resourceTypes?.map((option) => (
                              <SelectOption key={option} value={option}>
                                {option}
                              </SelectOption>
                            ))}
                          </KeycloakSelect>
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
                        render={({ field }) => (
                          <KeycloakSelect
                            className="keycloak__events_search__type_select"
                            data-testid="operation-types-searchField"
                            chipGroupProps={{
                              numChips: 1,
                              expandedText: t("hide"),
                              collapsedText: t("showRemaining"),
                            }}
                            variant={SelectVariant.typeaheadMulti}
                            typeAheadAriaLabel="Select"
                            onToggle={(isOpen) =>
                              setSelectOperationTypesOpen(isOpen)
                            }
                            selections={field.value}
                            onSelect={(selectedValue) => {
                              const option = selectedValue.toString();
                              const changedValue = field.value.includes(option)
                                ? field.value.filter(
                                    (item: string) => item !== option,
                                  )
                                : [...field.value, option];

                              field.onChange(changedValue);
                            }}
                            onClear={() => {
                              field.onChange([]);
                            }}
                            isOpen={selectOperationTypesOpen}
                            aria-labelledby={"operationTypes"}
                            chipGroupComponent={
                              <ChipGroup>
                                {field.value.map((chip: string) => (
                                  <Chip
                                    key={chip}
                                    onClick={(operation) => {
                                      operation.stopPropagation();
                                      field.onChange(
                                        field.value.filter(
                                          (val: string) => val !== chip,
                                        ),
                                      );
                                    }}
                                  >
                                    {chip}
                                  </Chip>
                                ))}
                              </ChipGroup>
                            }
                          >
                            {operationTypes?.map((option) => (
                              <SelectOption key={option} value={option}>
                                {option}
                              </SelectOption>
                            ))}
                          </KeycloakSelect>
                        )}
                      />
                    </FormGroup>
                    {!resourcePath && (
                      <TextControl
                        name="resourcePath"
                        label={t("resourcePath")}
                      />
                    )}
                    <TextControl name="authRealm" label={t("realm")} />
                    <TextControl name="authClient" label={t("client")} />
                    <TextControl name="authUser" label={t("userId")} />
                    <TextControl name="authIpAddress" label={t("ipAddress")} />
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
                </DropdownPanel>
              </FlexItem>
              <FlexItem>
                {Object.entries(activeFilters).length > 0 && (
                  <div className="keycloak__searchChips pf-v5-u-ml-md">
                    {Object.entries(activeFilters).map((filter) => {
                      const [key, value] = filter as [
                        keyof AdminEventSearchForm,
                        string | string[],
                      ];

                      if (key === "resourcePath" && !!resourcePath) {
                        return null;
                      }

                      return (
                        <ChipGroup
                          className="pf-v5-u-mt-md pf-v5-u-mr-md"
                          key={key}
                          categoryName={filterLabels[key]}
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
          </FormProvider>
        }
        actions={
          [
            {
              title: t("auth"),
              onRowClick: (event) => setAuthEvent(event),
            },
            {
              title: t("representation"),
              onRowClick: (event) => setRepresentationEvent(event),
            },
          ] as Action<AdminEventRepresentation>[]
        }
        columns={[
          {
            name: "time",
            displayKey: "time",
            cellRenderer: (row) =>
              formatDate(new Date(row.time!), FORMAT_DATE_AND_TIME),
          },
          {
            name: "resourcePath",
            displayKey: "resourcePath",
            cellRenderer: CellResourceLinkRenderer,
          },
          {
            name: "resourceType",
            displayKey: "resourceType",
          },
          {
            name: "operationType",
            displayKey: "operationType",
            transforms: [cellWidth(10)],
          },
          {
            name: "",
            displayKey: "user",
            cellRenderer: (event) => event.authDetails?.userId || "",
          },
        ]}
        emptyState={
          <ListEmptyState
            message={t("emptyAdminEvents")}
            instructions={t("emptyAdminEventsInstructions")}
            primaryActionText={t("refresh")}
            onPrimaryAction={() => setKey(key + 1)}
          />
        }
        isSearching={Object.keys(activeFilters).length > 0}
      />
    </>
  );
};
