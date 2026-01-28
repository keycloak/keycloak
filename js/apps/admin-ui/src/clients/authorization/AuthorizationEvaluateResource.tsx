import { useState } from "react";
import {
  ExpandableRowContent,
  Table,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { DescriptionList } from "@patternfly/react-core/dist/esm/components";
import { useTranslation } from "react-i18next";
import { AuthorizationEvaluateResourcePolicies } from "./AuthorizationEvaluateResourcePolicies";
import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import type PolicyResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyResultRepresentation";

type Props = {
  rowIndex: number;
  resource: EvaluationResultRepresentation;
  evaluateResults: any;
};

export const AuthorizationEvaluateResource = ({
  rowIndex,
  resource,
  evaluateResults,
}: Props) => {
  const [expanded, setExpanded] = useState<boolean>(false);
  const { t } = useTranslation();

  return (
    <Tbody isExpanded={expanded}>
      <Tr>
        <Td
          expand={{
            rowIndex,
            isExpanded: expanded,
            onToggle: () => setExpanded((prev) => !prev),
          }}
        />
        <Td data-testid={`name-column-${resource.resource}`}>
          {resource.resource?.name}
        </Td>
        <Td id={resource.status?.toLowerCase()}>
          {t(`${resource.status?.toLowerCase()}`)}
        </Td>
        <Td>
          {resource.allowedScopes?.length
            ? resource.allowedScopes.map((item) => item.name)
            : "-"}
        </Td>
      </Tr>
      <Tr key={`child-${resource.resource}`} isExpanded={expanded}>
        <Td />
        <Td colSpan={5}>
          <ExpandableRowContent>
            {expanded && (
              <DescriptionList
                isHorizontal
                className="keycloak_resource_details"
              >
                <Table aria-label={t("evaluationResults")}>
                  <Thead>
                    <Tr>
                      <Th aria-hidden="true" />
                      <Th>{t("permission")}</Th>
                      <Th>{t("results")}</Th>
                      <Th>{t("decisionStrategy")}</Th>
                      <Th>{t("grantedScopes")}</Th>
                      <Th>{t("deniedScopes")}</Th>
                      <Th aria-hidden="true" />
                    </Tr>
                  </Thead>
                  {Object.values(evaluateResults[rowIndex].policies).map(
                    (outerPolicy, idx) => (
                      <AuthorizationEvaluateResourcePolicies
                        key={idx}
                        idx={idx}
                        rowIndex={rowIndex}
                        outerPolicy={outerPolicy as PolicyResultRepresentation}
                        resource={resource}
                      />
                    ),
                  )}
                </Table>
              </DescriptionList>
            )}
          </ExpandableRowContent>
        </Td>
      </Tr>
    </Tbody>
  );
};
