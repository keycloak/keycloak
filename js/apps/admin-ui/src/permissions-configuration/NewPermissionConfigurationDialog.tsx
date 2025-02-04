import type { ResourceTypesRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useTranslation } from "react-i18next";
import {
  Modal,
  ModalVariant,
  TextContent,
  Text,
  TextVariants,
  Alert,
} from "@patternfly/react-core";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useMemo } from "react";
import useLocaleSort, { mapByKey } from "../utils/useLocaleSort";
import { isValidComponentType } from "./permission-configuration/PermissionConfigurationDetails";

type NewPermissionConfigurationDialogProps = {
  resourceTypes?: ResourceTypesRepresentation[];
  toggleDialog: () => void;
  onSelect: (resourceType: ResourceTypesRepresentation) => void;
};

export const NewPermissionConfigurationDialog = ({
  resourceTypes,
  onSelect,
  toggleDialog,
}: NewPermissionConfigurationDialogProps) => {
  const { t } = useTranslation();
  const localeSort = useLocaleSort();

  const sortedResourceTypes = useMemo(() => {
    const resourceTypeArray = resourceTypes
      ? (Object.values(resourceTypes) as ResourceTypesRepresentation[])
      : [];
    return localeSort(resourceTypeArray, mapByKey("type"));
  }, [resourceTypes, localeSort]);

  return (
    <Modal
      aria-label={t("createPermission")}
      variant={ModalVariant.medium}
      header={
        <TextContent>
          <Text component={TextVariants.h1}>{t("chooseAResourceType")}</Text>
          <Alert
            variant="info"
            isInline
            title={t("chooseAResourceTypeInstructions")}
            component="p"
          />
        </TextContent>
      }
      isOpen
      onClose={toggleDialog}
    >
      <Table aria-label={t("permissions")} variant="compact">
        <Thead>
          <Tr>
            <Th>{t("resourceType")}</Th>
            <Th>{t("description")}</Th>
          </Tr>
        </Thead>
        <Tbody>
          {sortedResourceTypes.map((resourceType) => (
            <Tr
              key={resourceType.type}
              data-testid={resourceType.type}
              onRowClick={() => {
                const transformedResourceType: ResourceTypesRepresentation = {
                  ...resourceType,
                  scopes: resourceType.scopes,
                };
                onSelect(transformedResourceType);
              }}
              isClickable
            >
              <Td>{resourceType.type}</Td>
              <Td style={{ textWrap: "wrap" }}>
                {isValidComponentType(resourceType.type!) &&
                  t(`resourceType.${resourceType.type}`)}
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </Modal>
  );
};
