import {
  Badge,
  Button,
  Chip,
  Modal,
  ModalVariant,
  Text,
} from "@patternfly/react-core";
import { UserCheckIcon } from "@patternfly/react-icons";

import {
  TableComposable,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useAlerts } from "ui-shared";
import { fetchPermission, updateRequest } from "../api";
import { Permission, Resource } from "../api/representations";

type PermissionRequestProps = {
  resource: Resource;
  refresh: () => void;
};

export const PermissionRequest = ({
  resource,
  refresh,
}: PermissionRequestProps) => {
  const { t } = useTranslation();
  const { addAlert, addError } = useAlerts();

  const [open, setOpen] = useState(false);

  const toggle = () => setOpen(!open);

  const approveDeny = async (
    shareRequest: Permission,
    approve: boolean = false
  ) => {
    try {
      const permissions = await fetchPermission({}, resource._id);
      const { scopes, username } = permissions.find(
        (p) => p.username === shareRequest.username
      )!;

      await updateRequest(
        resource._id,
        username,
        approve
          ? [...(scopes as string[]), ...(shareRequest.scopes as string[])]
          : scopes
      );
      addAlert(t("shareSuccess"));
      toggle();
      refresh();
    } catch (error) {
      addError(t("shareError", { error }).toString());
    }
  };

  return (
    <>
      <Button variant="link" onClick={toggle}>
        <UserCheckIcon size="lg" />
        <Badge>{resource.shareRequests.length}</Badge>
      </Button>
      <Modal
        title={t("permissionRequest", [resource.name])}
        variant={ModalVariant.large}
        isOpen={open}
        onClose={toggle}
        actions={[
          <Button key="close" variant="link" onClick={toggle}>
            {t("close")}
          </Button>,
        ]}
      >
        <TableComposable aria-label={t("resources")}>
          <Thead>
            <Tr>
              <Th>{t("requestor")}</Th>
              <Th>{t("permissionRequests")}</Th>
              <Th></Th>
            </Tr>
          </Thead>
          <Tbody>
            {resource.shareRequests.map((shareRequest) => (
              <Tr key={shareRequest.username}>
                <Td>
                  {shareRequest.firstName} {shareRequest.lastName}{" "}
                  {shareRequest.lastName ? "" : shareRequest.username}
                  <br />
                  <Text component="small">{shareRequest.email}</Text>
                </Td>
                <Td>
                  {shareRequest.scopes.map((scope) => (
                    <Chip key={scope.toString()} isReadOnly>
                      {scope as string}
                    </Chip>
                  ))}
                </Td>
                <Td>
                  <Button
                    onClick={() => {
                      approveDeny(shareRequest, true);
                    }}
                  >
                    {t("accept")}
                  </Button>
                  <Button
                    onClick={() => {
                      approveDeny(shareRequest);
                    }}
                    className="pf-u-ml-sm"
                    variant="danger"
                  >
                    {t("doDeny")}
                  </Button>
                </Td>
              </Tr>
            ))}
          </Tbody>
        </TableComposable>
      </Modal>
    </>
  );
};
