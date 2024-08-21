import OrganizationRepresentation from "@keycloak/keycloak-admin-client/lib/defs/organizationRepresentation";
import { Badge, Chip, ChipGroup } from "@patternfly/react-core";
import { TableText } from "@patternfly/react-table";
import { PropsWithChildren, ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import {
  KeycloakDataTable,
  LoaderFunction,
} from "@keycloak/keycloak-ui-shared";
import { useRealm } from "../context/realm-context/RealmContext";
import { toEditOrganization } from "./routes/EditOrganization";

const OrgDetailLink = (organization: OrganizationRepresentation) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  return (
    <TableText wrapModifier="truncate">
      <Link
        key={organization.id}
        to={toEditOrganization({
          realm,
          id: organization.id!,
          tab: "settings",
        })}
      >
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
      </Link>
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
      {org.domains?.map((dn) => (
        <Chip key={dn.name} isReadOnly>
          {dn.name}
        </Chip>
      ))}
    </ChipGroup>
  );
};

type OrganizationTableProps = PropsWithChildren & {
  loader:
    | LoaderFunction<OrganizationRepresentation>
    | OrganizationRepresentation[];
  toolbarItem?: ReactNode;
  isPaginated?: boolean;
  onSelect?: (orgs: OrganizationRepresentation[]) => void;
  onDelete?: (org: OrganizationRepresentation) => void;
  deleteLabel?: string;
};

export const OrganizationTable = ({
  loader,
  toolbarItem,
  isPaginated = false,
  onSelect,
  onDelete,
  deleteLabel = "delete",
  children,
}: OrganizationTableProps) => {
  const { t } = useTranslation();

  return (
    <KeycloakDataTable
      loader={loader}
      isPaginated={isPaginated}
      ariaLabelKey="organizationList"
      searchPlaceholderKey="searchOrganization"
      toolbarItem={toolbarItem}
      onSelect={onSelect}
      canSelectAll={onSelect !== undefined}
      actions={[
        {
          title: t(deleteLabel),
          onRowClick: onDelete,
        },
      ]}
      columns={[
        {
          name: "name",
          displayKey: "name",
          cellRenderer: OrgDetailLink,
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
      ]}
      emptyState={children}
    />
  );
};
