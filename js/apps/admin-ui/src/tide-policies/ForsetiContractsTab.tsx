/** TIDECLOAK IMPLEMENTATION */

import { useEffect, useState } from "react";
import {
  ClipboardCopy,
  ClipboardCopyVariant,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  EmptyStateIcon,
  PageSection,
  Spinner,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import {
  Table,
  Thead,
  Tr,
  Th,
  Tbody,
  Td,
  ExpandableRowContent,
} from "@patternfly/react-table";
import { useAdminClient } from "../admin-client";

interface ForsetiContract {
  id: string;
  contractHash: string;
  contractCode: string;
  name?: string;
  timestamp: number;
}

function truncateHash(hash: string, len = 24): string {
  if (!hash) return "-";
  return hash.length > len ? hash.substring(0, len) + "..." : hash;
}

function formatTimestamp(ms: number): string {
  return new Date(ms).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
}

export const ForsetiContractsTab = () => {
  const { adminClient } = useAdminClient();

  const [contracts, setContracts] = useState<ForsetiContract[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const data = await adminClient.tideUsersExt.listForsetiContracts();
        setContracts(data);
      } catch {
        setContracts([]);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [adminClient]);

  const toggleRow = (id: string) => {
    setExpandedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  if (loading) {
    return (
      <PageSection variant="light">
        <Spinner size="lg" />
      </PageSection>
    );
  }

  if (contracts.length === 0) {
    return (
      <PageSection variant="light">
        <EmptyState variant="lg">
          <EmptyStateHeader
            titleText="No Forseti Contracts"
            icon={<EmptyStateIcon icon={SearchIcon} />}
            headingLevel="h3"
          />
          <EmptyStateBody>
            Contract source code will appear here when SSH policies are
            committed.
          </EmptyStateBody>
        </EmptyState>
      </PageSection>
    );
  }

  return (
    <PageSection variant="light">
      <Table aria-label="Forseti Contracts" variant="compact">
        <Thead>
          <Tr>
            <Th screenReaderText="Row expansion" />
            <Th>Name</Th>
            <Th>Contract Hash</Th>
            <Th>Timestamp</Th>
          </Tr>
        </Thead>
        {contracts.map((contract) => {
          const isExpanded = expandedRows.has(contract.id);
          return (
            <Tbody key={contract.id} isExpanded={isExpanded}>
              <Tr>
                <Td
                  expand={{
                    rowIndex: 0,
                    isExpanded,
                    onToggle: () => toggleRow(contract.id),
                  }}
                />
                <Td dataLabel="Name">{contract.name || "Unnamed"}</Td>
                <Td dataLabel="Contract Hash">
                  <code title={contract.contractHash}>
                    {truncateHash(contract.contractHash)}
                  </code>
                </Td>
                <Td dataLabel="Timestamp">
                  {contract.timestamp
                    ? formatTimestamp(contract.timestamp)
                    : "-"}
                </Td>
              </Tr>
              <Tr isExpanded={isExpanded}>
                <Td colSpan={4}>
                  <ExpandableRowContent>
                    <ClipboardCopy
                      isCode
                      isReadOnly
                      hoverTip="Copy"
                      clickTip="Copied"
                      variant={ClipboardCopyVariant.expansion}
                    >
                      {contract.contractCode}
                    </ClipboardCopy>
                  </ExpandableRowContent>
                </Td>
              </Tr>
            </Tbody>
          );
        })}
      </Table>
    </PageSection>
  );
};
