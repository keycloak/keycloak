import {
  Breadcrumb,
  BreadcrumbItem,
  Button,
  DataList,
  DataListCell,
  DataListCheck,
  DataListItem,
  DataListItemCells,
  DataListItemRow,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import { OrganizationQuery } from "@keycloak/keycloak-admin-client/lib/resources/organizations";
import {
  ListEmptyState,
  PaginatingTableToolbar,
  useFetch,
} from "@keycloak/keycloak-ui-shared";
import { useOrganizationResource } from "../../context/organization-resource/OrganizationResourceContext";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { NetworkError } from "@keycloak/keycloak-admin-client";

export type OrganizationPickerDialogProps = {
  type: "selectOne" | "selectMany";
  filterOrganizations?: OrganizationRepresentation[];
  text: { title: string; ok: string };
  isMove?: boolean;
  onConfirm: (organizations: OrganizationRepresentation[] | undefined) => void;
  onClose: () => void;
};

type SelectableOrganization = OrganizationRepresentation & {
  checked?: boolean;
};

export const OrganizationPickerDialog = ({
  type,
  filterOrganizations,
  text,
  isMove = false,
  onClose,
  onConfirm,
}: OrganizationPickerDialogProps) => {
  const organizationResource = useOrganizationResource();

  const { t } = useTranslation();
  const [selectedRows, setSelectedRows] = useState<SelectableOrganization[]>(
    [],
  );

  const [navigation, setNavigation] = useState<SelectableOrganization[]>([]);
  const [organizations, setOrganizations] = useState<SelectableOrganization[]>(
    [],
  );
  const [filter, setFilter] = useState("");
  const [organizationId, setOrganizationId] = useState<string>();

  const [max, setMax] = useState(10);
  const [first, setFirst] = useState(0);

  const [count, setCount] = useState(0);

  const currentOrganization = () => navigation[navigation.length - 1];

  useFetch(
    async () => {
      let organization;
      let organizations: OrganizationRepresentation[] = [];

      if (!organizationId) {
        const args: OrganizationQuery = {
          first,
          max: max + 1,
        };
        if (filter !== "") {
          args.search = filter;
        }
        organizations = await organizationResource.find(args);
      } else {
        if (!navigation.map(({ id }) => id).includes(organizationId)) {
          try {
            organization = await organizationResource.findOne({
              id: organizationId,
            });
          } catch (error) {
            if (
              error instanceof NetworkError &&
              error.response.status === 403
            ) {
              organization = undefined;
            } else {
              throw error;
            }
          }
          if (!organization) {
            throw new Error(t("notFound"));
          }
        }
      }

      return { organization, organizations };
    },
    async ({ organization: selectedOrganization, organizations }) => {
      if (selectedOrganization) {
        setNavigation([...navigation, selectedOrganization]);
      }

      organizations.forEach((organization: SelectableOrganization) => {
        organization.checked = !!selectedRows.find(
          (r) => r.id === organization.id,
        );
      });
      setOrganizations(organizations);
      if (filter !== "" || !organizationId) {
        setCount(organizations.length);
      }
    },
    [organizationId, filter, first, max],
  );

  const isRowDisabled = (row?: OrganizationRepresentation) => {
    return [
      ...(filterOrganizations || []).map((organization) => organization.id),
    ].some((organization) => organization === row?.id);
  };

  return (
    <Modal
      variant={filter !== "" ? ModalVariant.medium : ModalVariant.small}
      title={t(text.title, {
        organization1: filterOrganizations?.[0]?.name,
        organization2: navigation.length
          ? currentOrganization().name
          : t("root"),
      })}
      isOpen
      onClose={onClose}
      actions={[
        <Button
          data-testid={`${text.ok}-button`}
          key="confirm"
          variant="primary"
          form="organization-form"
          onClick={() => {
            onConfirm(
              type === "selectMany"
                ? selectedRows
                : navigation.length
                  ? [currentOrganization()]
                  : undefined,
            );
          }}
          isDisabled={type === "selectMany" && selectedRows.length === 0}
        >
          {t(text.ok)}
        </Button>,
      ]}
    >
      <PaginatingTableToolbar
        count={count}
        first={first}
        max={max}
        onNextClick={setFirst}
        onPreviousClick={setFirst}
        onPerPageSelect={(first, max) => {
          setFirst(first);
          setMax(max);
        }}
        inputGroupName={"search"}
        inputGroupOnEnter={(search) => {
          setFilter(search);
          setFirst(0);
          setMax(10);
          setNavigation([]);
          setOrganizationId(undefined);
        }}
        inputGroupPlaceholder={t("searchForOrganizations")}
      >
        <Breadcrumb>
          {navigation.length > 0 && (
            <BreadcrumbItem key="home">
              <Button
                variant="link"
                onClick={() => {
                  setOrganizationId(undefined);
                  setNavigation([]);
                  setFirst(0);
                  setMax(10);
                }}
              >
                {t("organizations")}
              </Button>
            </BreadcrumbItem>
          )}
          {navigation.map((organization, i) => (
            <BreadcrumbItem key={i}>
              {navigation.length - 1 !== i && (
                <Button
                  variant="link"
                  onClick={() => {
                    setOrganizationId(organization.id);
                    setNavigation([...navigation].slice(0, i));
                    setFirst(0);
                    setMax(10);
                  }}
                >
                  {organization.name}
                </Button>
              )}
              {navigation.length - 1 === i && organization.name}
            </BreadcrumbItem>
          ))}
        </Breadcrumb>
        <DataList aria-label={t("organizations")} isCompact>
          {filter == ""
            ? organizations
                .slice(0, max)
                .map((organization: SelectableOrganization) => (
                  <OrganizationRow
                    key={organization.id}
                    organization={organization}
                    isRowDisabled={isRowDisabled}
                    onSelect={(organization) => {
                      setOrganizationId(organization.id);
                      setFirst(0);
                    }}
                    type={type}
                    selectedRows={selectedRows}
                    setSelectedRows={setSelectedRows}
                  />
                ))
            : organizations.map((o) => (
                <OrganizationRow
                  key={o.id}
                  organization={o}
                  isRowDisabled={isRowDisabled}
                  onSelect={(organization) => {
                    setOrganizationId(organization.id);
                    setFilter("");
                    setFirst(0);
                  }}
                  type={type}
                  selectedRows={selectedRows}
                  setSelectedRows={setSelectedRows}
                />
              ))}
        </DataList>
        {organizations.length === 0 && filter === "" && (
          <ListEmptyState
            hasIcon={false}
            message={t("moveOrganizationEmpty")}
            instructions={
              isMove ? t("moveOrganizationEmptyInstructions") : undefined
            }
          />
        )}
        {organizations.length === 0 && filter !== "" && (
          <ListEmptyState
            message={t("noSearchResults")}
            instructions={t("noSearchResultsInstructions")}
          />
        )}
      </PaginatingTableToolbar>
    </Modal>
  );
};

type OrganizationRowProps = {
  organization: SelectableOrganization;
  type: "selectOne" | "selectMany";
  isRowDisabled: (row?: OrganizationRepresentation) => boolean;
  onSelect?: (organization: OrganizationRepresentation) => void;
  selectedRows: SelectableOrganization[];
  setSelectedRows: (organizations: SelectableOrganization[]) => void;
};

const OrganizationRow = ({
  organization,
  type,
  isRowDisabled,
  onSelect,
  selectedRows,
  setSelectedRows,
}: OrganizationRowProps) => {
  return (
    <DataListItem
      aria-labelledby={organization.name}
      key={organization.id}
      id={organization.id}
      onClick={() => {
        if (type === "selectOne") {
          onSelect?.(organization);
        }
      }}
    >
      <DataListItemRow
        className={`join-organization-dialog-row${
          isRowDisabled(organization) ? "-m-disabled" : ""
        }`}
        data-testid={organization.name}
      >
        {type === "selectMany" && (
          <DataListCheck
            className="kc-join-organization-modal-check"
            data-testid={`${organization.name}-check`}
            aria-label={organization.name}
            checked={organization.checked}
            isDisabled={isRowDisabled(organization)}
            onChange={(_event, checked) => {
              organization.checked = checked;
              let newSelectedRows: SelectableOrganization[] = [];
              if (!organization.checked) {
                newSelectedRows = selectedRows.filter(
                  (r) => r.id !== organization.id,
                );
              } else {
                newSelectedRows = [...selectedRows, organization];
              }

              setSelectedRows(newSelectedRows);
            }}
            aria-labelledby={`select-${organization.name}`}
          />
        )}

        <DataListItemCells
          dataListCells={[
            <DataListCell
              key={`name-${organization.id}`}
              className="keycloak-organizations-organization-path"
            >
              <span id={`select-${organization.name}`}>
                {organization.name}
              </span>
            </DataListCell>,
          ]}
        />
      </DataListItemRow>
    </DataListItem>
  );
};
