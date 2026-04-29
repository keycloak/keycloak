import { ResourceTypesRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/resourceServerRepresentation";
import { useTranslation } from "react-i18next";
import { Content, ContentVariants, Alert } from "@patternfly/react-core";
import { Modal, ModalVariant } from "@patternfly/react-core/deprecated";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";

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

  return (
    <Modal
      aria-label={t("createPermission")}
      variant={ModalVariant.medium}
      header={
        <Content>
          <Content component={ContentVariants.h1}>
            {t("chooseAResourceType")}
          </Content>
          <Alert
            variant="info"
            isInline
            title={t("chooseAResourceTypeInstructions")}
            component="p"
          />
        </Content>
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
          {Object.keys(resourceTypes || {}).map((key: any) => {
            const resourceType = resourceTypes![key];
            return (
              <Tr
                key={resourceType.type}
                data-testid={resourceType.type}
                onRowClick={() => {
                  onSelect(resourceType);
                }}
                isClickable
              >
                <Td>{resourceType.type}</Td>
                <Td style={{ textWrap: "wrap" }}>
                  {t(`resourceType.${resourceType.type}`)}
                </Td>
              </Tr>
            );
          })}
        </Tbody>
      </Table>
    </Modal>
  );
};
