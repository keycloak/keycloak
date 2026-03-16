/** TIDECLOAK IMPLEMENTATION */

import { useEffect, useState } from "react";
import {
  Card,
  CardTitle,
  CardBody,
  CodeBlock,
  CodeBlockCode,
  DescriptionList,
  DescriptionListGroup,
  DescriptionListTerm,
  DescriptionListDescription,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  EmptyStateIcon,
  ExpandableSection,
  Label,
  PageSection,
  Spinner,
  Title,
} from "@patternfly/react-core";
import { SearchIcon } from "@patternfly/react-icons";
import { useAdminClient } from "../admin-client";
import { useRealm } from "../context/realm-context/RealmContext";
import {
  fetchAdminPolicy,
  parseAdminPolicy,
  type ParsedAdminPolicy,
} from "./utils/adminPolicyUtils";

interface PolicyEntry {
  id: string;
  roleId: string;
  contractId?: string;
  contractHash?: string;
  contractName?: string;
  contractCode?: string;
  approvalType: string;
  executionType: string;
  threshold: number;
  policyData?: string;
  timestamp: number;
}

interface RolePolicyEntry {
  id: string;
  roleId: string;
  roleName: string;
  clientRole: boolean;
  clientId?: string;
  timestamp: number;
  hasSig: boolean;
  policyDisplay?: string;
  parsed?: ParsedAdminPolicy;
}

function truncateHash(hash: string, len = 16): string {
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

function PolicyDetails({ policy }: { policy: PolicyEntry }) {
  const [codeExpanded, setCodeExpanded] = useState(false);

  return (
    <>
    <DescriptionList
      isHorizontal
      columnModifier={{ default: "2Col" }}
      className="pf-v5-u-mt-sm"
    >
      <DescriptionListGroup>
        <DescriptionListTerm>Role</DescriptionListTerm>
        <DescriptionListDescription>
          <code>{policy.roleId}</code>
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Approval Type</DescriptionListTerm>
        <DescriptionListDescription>
          <Label color="blue">{policy.approvalType}</Label>
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Execution Type</DescriptionListTerm>
        <DescriptionListDescription>
          <Label color="blue">{policy.executionType}</Label>
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Threshold</DescriptionListTerm>
        <DescriptionListDescription>
          {policy.threshold}
        </DescriptionListDescription>
      </DescriptionListGroup>
      {(policy.contractName || policy.contractHash) && (
        <DescriptionListGroup>
          <DescriptionListTerm>Contract</DescriptionListTerm>
          <DescriptionListDescription>
            <code title={policy.contractHash}>
              {policy.contractName || truncateHash(policy.contractHash || "")}
            </code>
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      {policy.contractHash && (
        <DescriptionListGroup>
          <DescriptionListTerm>Contract Hash</DescriptionListTerm>
          <DescriptionListDescription>
            <code title={policy.contractHash}>
              {truncateHash(policy.contractHash, 24)}
            </code>
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      {policy.timestamp > 0 && (
        <DescriptionListGroup>
          <DescriptionListTerm>Created</DescriptionListTerm>
          <DescriptionListDescription>
            {formatTimestamp(policy.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
    </DescriptionList>
    {policy.contractCode && (
      <ExpandableSection
        toggleText={codeExpanded ? "Hide Contract Source" : "Show Contract Source"}
        isExpanded={codeExpanded}
        onToggle={() => setCodeExpanded((prev) => !prev)}
        className="pf-v5-u-mt-md"
      >
        <CodeBlock>
          <CodeBlockCode>{policy.contractCode}</CodeBlockCode>
        </CodeBlock>
      </ExpandableSection>
    )}
    </>
  );
}

function RolePolicyDetails({ policy }: { policy: RolePolicyEntry }) {
  if (!policy.parsed) {
    return <code>{policy.policyDisplay || "Unable to parse policy"}</code>;
  }

  const p = policy.parsed;

  return (
    <DescriptionList
      isHorizontal
      columnModifier={{ default: "2Col" }}
      className="pf-v5-u-mt-sm"
    >
      <DescriptionListGroup>
        <DescriptionListTerm>Role ID</DescriptionListTerm>
        <DescriptionListDescription>
          <code>{policy.roleId}</code>
        </DescriptionListDescription>
      </DescriptionListGroup>
      {policy.clientRole && policy.clientId && (
        <DescriptionListGroup>
          <DescriptionListTerm>Client</DescriptionListTerm>
          <DescriptionListDescription>
            <code>{policy.clientId}</code>
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      <DescriptionListGroup>
        <DescriptionListTerm>Contract ID</DescriptionListTerm>
        <DescriptionListDescription>
          <code>{p.contractId}</code>
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Model ID</DescriptionListTerm>
        <DescriptionListDescription>
          {p.modelId}
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Approval Type</DescriptionListTerm>
        <DescriptionListDescription>
          <Label color="blue">{p.approvalType}</Label>
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Execution Type</DescriptionListTerm>
        <DescriptionListDescription>
          <Label color="blue">{p.executionType}</Label>
        </DescriptionListDescription>
      </DescriptionListGroup>
      <DescriptionListGroup>
        <DescriptionListTerm>Key ID</DescriptionListTerm>
        <DescriptionListDescription>
          <code title={p.keyId}>{truncateHash(p.keyId, 24)}</code>
        </DescriptionListDescription>
      </DescriptionListGroup>
      {p.params.threshold && (
        <DescriptionListGroup>
          <DescriptionListTerm>Threshold</DescriptionListTerm>
          <DescriptionListDescription>
            {p.params.threshold}
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      {p.params.resource && (
        <DescriptionListGroup>
          <DescriptionListTerm>Resource</DescriptionListTerm>
          <DescriptionListDescription>
            <code>{p.params.resource}</code>
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      {p.params.role && (
        <DescriptionListGroup>
          <DescriptionListTerm>Policy Role</DescriptionListTerm>
          <DescriptionListDescription>
            <code>{p.params.role}</code>
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      <DescriptionListGroup>
        <DescriptionListTerm>Signed</DescriptionListTerm>
        <DescriptionListDescription>
          <Label color={policy.hasSig ? "green" : "orange"}>
            {policy.hasSig ? "Yes" : "No"}
          </Label>
        </DescriptionListDescription>
      </DescriptionListGroup>
      {p.signature && (
        <DescriptionListGroup>
          <DescriptionListTerm>Signature</DescriptionListTerm>
          <DescriptionListDescription>
            <code title={p.signature}>{truncateHash(p.signature, 24)}</code>
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
      {policy.timestamp > 0 && (
        <DescriptionListGroup>
          <DescriptionListTerm>Created</DescriptionListTerm>
          <DescriptionListDescription>
            {formatTimestamp(policy.timestamp)}
          </DescriptionListDescription>
        </DescriptionListGroup>
      )}
    </DescriptionList>
  );
}

export const OverviewTab = () => {
  const { adminClient } = useAdminClient();
  const { realm } = useRealm();

  const [adminPolicy, setAdminPolicy] = useState<ParsedAdminPolicy | null>(null);
  const [adminPolicyError, setAdminPolicyError] = useState<string | null>(null);
  const [adminPolicyLoading, setAdminPolicyLoading] = useState(true);

  const [policies, setPolicies] = useState<PolicyEntry[]>([]);
  const [policiesLoading, setPoliciesLoading] = useState(true);
  const [expandedPolicies, setExpandedPolicies] = useState<
    Record<string, boolean>
  >({});

  const [rolePolicies, setRolePolicies] = useState<RolePolicyEntry[]>([]);
  const [rolePoliciesLoading, setRolePoliciesLoading] = useState(true);
  const [expandedRolePolicies, setExpandedRolePolicies] = useState<
    Record<string, boolean>
  >({});

  useEffect(() => {
    const loadAdminPolicy = async () => {
      setAdminPolicyLoading(true);
      try {
        const token = await adminClient.getAccessToken();
        const raw = await fetchAdminPolicy(adminClient.baseUrl, realm, token);
        const parsed = parseAdminPolicy(raw);
        setAdminPolicy(parsed);
        setAdminPolicyError(null);
      } catch (err) {
        console.warn("Failed to load admin policy:", err);
        setAdminPolicy(null);
        setAdminPolicyError("No admin policy configured for this realm");
      } finally {
        setAdminPolicyLoading(false);
      }
    };

    const loadPolicies = async () => {
      setPoliciesLoading(true);
      try {
        const data = await adminClient.tideUsersExt.listSshPolicies();
        setPolicies(data);
      } catch {
        setPolicies([]);
      } finally {
        setPoliciesLoading(false);
      }
    };

    const loadRolePolicies = async () => {
      setRolePoliciesLoading(true);
      try {
        const data = await adminClient.tideUsersExt.listRolePolicies();
        const withParsed = data.map((rp) => {
          let parsed: ParsedAdminPolicy | undefined;
          if (rp.policyDisplay) {
            try {
              parsed = parseAdminPolicy(rp.policyDisplay);
            } catch {
              // leave parsed undefined
            }
          }
          return { ...rp, parsed };
        });
        setRolePolicies(withParsed);
      } catch {
        setRolePolicies([]);
      } finally {
        setRolePoliciesLoading(false);
      }
    };

    loadAdminPolicy();
    loadPolicies();
    loadRolePolicies();
  }, [adminClient, realm]);

  const togglePolicy = (id: string) => {
    setExpandedPolicies((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const toggleRolePolicy = (id: string) => {
    setExpandedRolePolicies((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <PageSection variant="light">
      {/* Admin Policy Card */}
      <Card className="pf-v5-u-mb-lg">
        <CardTitle>
          <Title headingLevel="h2" size="lg">
            Admin Policy
          </Title>
        </CardTitle>
        <CardBody>
          {adminPolicyLoading ? (
            <Spinner size="md" />
          ) : adminPolicy ? (
            <>
              <Label color="green" className="pf-v5-u-mb-md">
                Active
              </Label>
              <DescriptionList
                isHorizontal
                columnModifier={{ default: "2Col" }}
                className="pf-v5-u-mt-sm"
              >
                <DescriptionListGroup>
                  <DescriptionListTerm>Version</DescriptionListTerm>
                  <DescriptionListDescription>
                    {adminPolicy.version}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>Contract ID</DescriptionListTerm>
                  <DescriptionListDescription>
                    <code>{adminPolicy.contractId}</code>
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>Model ID</DescriptionListTerm>
                  <DescriptionListDescription>
                    {adminPolicy.modelId}
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>Approval Type</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Label color="blue">{adminPolicy.approvalType}</Label>
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>Execution Type</DescriptionListTerm>
                  <DescriptionListDescription>
                    <Label color="blue">{adminPolicy.executionType}</Label>
                  </DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                  <DescriptionListTerm>Key ID</DescriptionListTerm>
                  <DescriptionListDescription>
                    <code title={adminPolicy.keyId}>
                      {truncateHash(adminPolicy.keyId, 24)}
                    </code>
                  </DescriptionListDescription>
                </DescriptionListGroup>
                {adminPolicy.params.threshold && (
                  <DescriptionListGroup>
                    <DescriptionListTerm>Threshold</DescriptionListTerm>
                    <DescriptionListDescription>
                      {adminPolicy.params.threshold}
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
                {adminPolicy.params.resource && (
                  <DescriptionListGroup>
                    <DescriptionListTerm>Resource</DescriptionListTerm>
                    <DescriptionListDescription>
                      <code>{adminPolicy.params.resource}</code>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
                {adminPolicy.params.role && (
                  <DescriptionListGroup>
                    <DescriptionListTerm>Role</DescriptionListTerm>
                    <DescriptionListDescription>
                      <code>{adminPolicy.params.role}</code>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
                {adminPolicy.signature && (
                  <DescriptionListGroup>
                    <DescriptionListTerm>Signature</DescriptionListTerm>
                    <DescriptionListDescription>
                      <code title={adminPolicy.signature}>
                        {truncateHash(adminPolicy.signature, 24)}
                      </code>
                    </DescriptionListDescription>
                  </DescriptionListGroup>
                )}
              </DescriptionList>
            </>
          ) : (
            <EmptyState variant="xs">
              <EmptyStateHeader
                titleText="No admin policy configured"
                icon={<EmptyStateIcon icon={SearchIcon} />}
                headingLevel="h3"
              />
              <EmptyStateBody>
                {adminPolicyError ||
                  "The admin policy has not been set for this realm."}
              </EmptyStateBody>
            </EmptyState>
          )}
        </CardBody>
      </Card>

      {/* Role Policies (merged: initCert + SSH/Forseti) */}
      <Card>
        <CardTitle>
          <Title headingLevel="h2" size="lg">
            Role Policies ({rolePolicies.length + policies.length})
          </Title>
        </CardTitle>
        <CardBody>
          {(rolePoliciesLoading || policiesLoading) ? (
            <Spinner size="md" />
          ) : (rolePolicies.length + policies.length) > 0 ? (
            <>
              {rolePolicies.map((rp) => {
                const threshold = rp.parsed?.params.threshold || "?";
                const scope = rp.clientRole && rp.clientId
                  ? `${rp.clientId} / ${rp.roleName}`
                  : rp.roleName;

                return (
                  <ExpandableSection
                    key={rp.id}
                    toggleText={`${scope}  ·  Threshold: ${threshold}`}
                    isExpanded={!!expandedRolePolicies[rp.id]}
                    onToggle={() => toggleRolePolicy(rp.id)}
                    className="pf-v5-u-mb-sm"
                  >
                    <RolePolicyDetails policy={rp} />
                  </ExpandableSection>
                );
              })}
              {policies.map((policy) => {
                const role = policy.roleId;
                const approval = policy.approvalType;
                const threshold = String(policy.threshold);
                const contract =
                  policy.contractName ||
                  truncateHash(policy.contractHash || "", 20);

                return (
                  <ExpandableSection
                    key={policy.id}
                    toggleText={`${role}  ·  ${approval}  ·  Threshold: ${threshold}  ·  ${contract}`}
                    isExpanded={!!expandedPolicies[policy.id]}
                    onToggle={() => togglePolicy(policy.id)}
                    className="pf-v5-u-mb-sm"
                  >
                    <PolicyDetails policy={policy} />
                  </ExpandableSection>
                );
              })}
            </>
          ) : (
            <EmptyState variant="xs">
              <EmptyStateHeader
                titleText="No role policies"
                icon={<EmptyStateIcon icon={SearchIcon} />}
                headingLevel="h3"
              />
              <EmptyStateBody>
                Policies will appear here when configured on roles.
              </EmptyStateBody>
            </EmptyState>
          )}
        </CardBody>
      </Card>
    </PageSection>
  );
};
