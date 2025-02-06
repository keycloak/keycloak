import type EvaluationResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/evaluationResultRepresentation";
import { DecisionEffect } from "@keycloak/keycloak-admin-client/lib/defs/policyRepresentation";
import type PolicyResultRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyResultRepresentation";
import {
  capitalize,
  DescriptionList,
  TextContent,
  TextList,
  TextListItem,
} from "@patternfly/react-core";
import { ExpandableRowContent, Tbody, Td, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { useRealm } from "../../context/realm-context/RealmContext";
import { useParams } from "../../utils/useParams";
import type { ClientParams } from "../routes/Client";
import { toPermissionDetails } from "../routes/PermissionDetails";
import { toPolicyDetails } from "../routes/PolicyDetails";

type Props = {
  idx: number;
  rowIndex: number;
  outerPolicy: PolicyResultRepresentation;
  resource: EvaluationResultRepresentation;
};

export const AuthorizationEvaluateResourcePolicies = ({
  idx,
  rowIndex,
  outerPolicy,
  resource,
}: Props) => {
  const [expanded, setExpanded] = useState<boolean>(false);
  const { t } = useTranslation();
  const { realm } = useRealm();
  const { clientId } = useParams<ClientParams>();

  return (
    <Tbody key={idx} isExpanded={expanded}>
      <Tr>
        <Td
          expand={{
            rowIndex,
            isExpanded: expanded,
            onToggle: () => setExpanded((prev) => !prev),
          }}
        />
        <Td data-testid={`name-column-${resource.resource}`}>
          <Link
            to={toPermissionDetails({
              realm,
              id: clientId,
              permissionType: outerPolicy.policy?.type!,
              permissionId: outerPolicy.policy?.id!,
            })}
          >
            {outerPolicy.policy?.name}
          </Link>
        </Td>
        <Td id={outerPolicy.status?.toLowerCase()}>
          {t(outerPolicy.status?.toLowerCase() as string)}
        </Td>
        <Td>{t(`${outerPolicy.policy?.decisionStrategy?.toLowerCase()}`)}</Td>
        <Td>
          {outerPolicy.status === DecisionEffect.Permit
            ? resource.policies?.[rowIndex]?.scopes?.join(", ")
            : "-"}
        </Td>
        <Td>
          {outerPolicy.status === DecisionEffect.Deny &&
          resource.policies?.[rowIndex]?.scopes?.length
            ? resource.policies[rowIndex].scopes?.join(", ")
            : "-"}
        </Td>
      </Tr>
      <Tr key={`child-${resource.resource}`} isExpanded={expanded}>
        <Td />
        <Td colSpan={5}>
          {expanded && (
            <ExpandableRowContent>
              <DescriptionList
                isHorizontal
                className="keycloak_resource_details"
              >
                <TextContent>
                  <TextList>
                    {outerPolicy.associatedPolicies?.map((item) => (
                      <TextListItem key="policyDetails">
                        <Link
                          to={toPolicyDetails({
                            realm,
                            id: clientId,
                            policyType: item.policy?.type!,
                            policyId: item.policy?.id!,
                          })}
                        >
                          {item.policy?.name}
                        </Link>{" "}
                        {t("votedToStatus", {
                          status: capitalize(item.status as string),
                        })}
                      </TextListItem>
                    ))}
                  </TextList>
                </TextContent>
              </DescriptionList>
            </ExpandableRowContent>
          )}
        </Td>
      </Tr>
    </Tbody>
  );
};
