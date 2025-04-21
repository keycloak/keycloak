import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import { Badge, Chip, ChipGroup } from "@patternfly/react-core";
import { TableText } from "@patternfly/react-table";
import { FunctionComponent, PropsWithChildren, ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { KeycloakDataTable, LoaderFunction } from "./table/KeycloakDataTable";

type OrgDetailLinkProps = {
  link: FunctionComponent<
    PropsWithChildren<{ organization: OrganizationRepresentation }>
  >;
  organization: OrganizationRepresentation;
};

const OrgDetailLink = ({ link, organization }: OrgDetailLinkProps) => {
  const { t } = useTranslation();
  const Component = link;
  return (
    <TableText wrapModifier="truncate">
      <Component organization={organization}>
        {organization.name}
        {!organization.enabled && (
          <Badge
            key={`${organization.id}-disabled`}
            isRead
            className="pf-v5-u-ml-sm"
          >
            {t("disabled")}
          </Badge>
        )}
      </Component>
    </TableText>
  );
};

const Domains = (org: OrganizationRepresentation) => {
  const { t } = useTranslation();
  return (
    <ChipGroup
      numChips={2}
      expandedText={t("hide")}
      collapsedText={t("showRemaining")}
    >
      {org.domains?.map((dn) => {
        const name = typeof dn === "string" ? dn : dn.name;
        return (
          <Chip key={name} isReadOnly>
            {name}
          </Chip>
        );
      })}
    </ChipGroup>
  );
};

export type OrganizationTableProps = PropsWithChildren & {
  loader:
    | LoaderFunction<OrganizationRepresentation>
    | OrganizationRepresentation[];
  link: FunctionComponent<
    PropsWithChildren<{ organization: OrganizationRepresentation }>
  >;
  toolbarItem?: ReactNode;
  isPaginated?: boolean;
  isSearching?: boolean;
  searchPlaceholderKey?: string;
  onSelect?: (orgs: OrganizationRepresentation[]) => void;
  onDelete?: (org: OrganizationRepresentation) => void;
  deleteLabel?: string;
};

export const OrganizationTable = ({
  loader,
  toolbarItem,
  isPaginated = false,
  isSearching = false,
  searchPlaceholderKey,
  onSelect,
  onDelete,
  deleteLabel = "delete",
  link,
  children,
}: OrganizationTableProps) => {
  const { t } = useTranslation();

  return (
    <KeycloakDataTable
      loader={loader}
      isPaginated={isPaginated}
      isSearching={isSearching}
      ariaLabelKey="organizationList"
      searchPlaceholderKey={searchPlaceholderKey}
      toolbarItem={toolbarItem}
      onSelect={onSelect}
      canSelectAll={onSelect !== undefined}
      actions={
        onDelete
          ? [
              {
                title: t(deleteLabel),
                onRowClick: onDelete,
              },
            ]
          : undefined
      }
      columns={[
        {
          name: "name",
          displayKey: "name",
          cellRenderer: (row) => (
            <OrgDetailLink link={link} organization={row} />
          ),
        },
        {
          name: "domains",
          displayKey: "domains",
          cellRenderer: Domains,
        },
        {
          name: "description",
          displayKey: "description",
        },
        {
          name: "membershipType",
          displayKey: "membershipType",
        },
      ]}
      emptyState={children}
    />
  );
};
