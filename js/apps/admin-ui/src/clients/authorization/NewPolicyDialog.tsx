import { useTranslation } from "react-i18next";
import {
  Modal,
  ModalVariant,
  TextContent,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";

import type PolicyProviderRepresentation from "@keycloak/keycloak-admin-client/lib/defs/policyProviderRepresentation";
import { isValidComponentType } from "./policy/PolicyDetails";
import { useMemo } from "react";
import useLocaleSort, { mapByKey } from "../../utils/useLocaleSort";

type NewPolicyDialogProps = {
  policyProviders?: PolicyProviderRepresentation[];
  toggleDialog: () => void;
  onSelect: (provider: PolicyProviderRepresentation) => void;
};

export const NewPolicyDialog = ({
  policyProviders,
  onSelect,
  toggleDialog,
}: NewPolicyDialogProps) => {
  const { t } = useTranslation();
  const localeSort = useLocaleSort();

  const sortedPolicies = useMemo(
    () =>
      policyProviders ? localeSort(policyProviders, mapByKey("name")) : [],
    [policyProviders],
  );

  return (
    <Modal
      aria-label={t("createPolicy")}
      variant={ModalVariant.medium}
      header={
        <TextContent>
          <Text component={TextVariants.h1}>{t("chooseAPolicyType")}</Text>
          <Text>{t("chooseAPolicyTypeInstructions")}</Text>
        </TextContent>
      }
      isOpen
      onClose={toggleDialog}
    >
      <Table aria-label={t("policies")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("name")}</Th>
            <Th>{t("description")}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {sortedPolicies.map((provider) => (
            <Tr
              key={provider.type}
              data-testid={provider.type}
              onRowClick={() => onSelect(provider)}
              isClickable
            >
              <Td>{provider.name}</Td>
              <Td>
                {isValidComponentType(provider.type!) &&
                  t(`policyProvider.${provider.type}`)}
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </Modal>
  );
};
