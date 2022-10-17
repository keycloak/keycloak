import { useTranslation } from "react-i18next";
import {
  Modal,
  ModalVariant,
  TextContent,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";

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
  const { t } = useTranslation("clients");
  const localeSort = useLocaleSort();

  const sortedPolicies = useMemo(
    () =>
      policyProviders ? localeSort(policyProviders, mapByKey("name")) : [],
    [policyProviders]
  );

  return (
    <Modal
      aria-labelledby={t("addPredefinedMappers")}
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
      <TableComposable aria-label={t("policies")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("common:name")}</Th>
            <Th>{t("common:description")}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {sortedPolicies.map((provider) => (
            <Tr
              key={provider.type}
              data-testid={provider.type}
              onRowClick={() => onSelect(provider)}
              isHoverable
            >
              <Td>{provider.name}</Td>
              <Td>
                {isValidComponentType(provider.type!) &&
                  t(`policyProvider.${provider.type}`)}
              </Td>
            </Tr>
          ))}
        </Tbody>
      </TableComposable>
    </Modal>
  );
};
