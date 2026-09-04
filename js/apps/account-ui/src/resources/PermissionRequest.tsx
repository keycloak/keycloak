import { useEnvironment } from "@keycloak/keycloak-ui-shared";
import {
  Label,
  Badge,
  Button,
  Icon,
  Content,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalVariant,
} from "@patternfly/react-core";
import { UserCheckIcon } from "@patternfly/react-icons";
import { Table, Tbody, Td, Th, Thead, Tr } from "@patternfly/react-table";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import { fetchPermission, updateRequest } from "../api";
import { Permission, Resource } from "../api/representations";
import { useAccountAlerts } from "../utils/useAccountAlerts";

type PermissionRequestProps = {
  resource: Resource;
  refresh: () => void;
};

export const PermissionRequest = ({
  resource,
  refresh,
}: PermissionRequestProps) => {
  const { t } = useTranslation();
  const context = useEnvironment();
  const { addAlert, addError } = useAccountAlerts();

  const [open, setOpen] = useState(false);

  const toggle = () => setOpen(!open);

  const approveDeny = async (
    shareRequest: Permission,
    approve: boolean = false,
  ) => {
    try {
      const permissions = await fetchPermission({ context }, resource._id);
      const { scopes, username } = permissions.find(
        (p) => p.username === shareRequest.username,
      ) || { scopes: [], username: shareRequest.username };

      await updateRequest(
        context,
        resource._id,
        username,
        approve
          ? [...(scopes as string[]), ...(shareRequest.scopes as string[])]
          : scopes,
      );
      addAlert(t("shareSuccess"));
      toggle();
      refresh();
    } catch (error) {
      addError("shareError", error);
    }
  };

  const modalTitle = t("permissionRequest", { name: resource.name });

  return (
    <>
      <Button
        icon={
          <Icon size="lg">
            <UserCheckIcon />
          </Icon>
        }
        variant="link"
        onClick={toggle}
      >
        <Badge>{resource.shareRequests?.length}</Badge>
      </Button>
      <Modal
        variant={ModalVariant.large}
        isOpen={open}
        onClose={toggle}
        aria-label={modalTitle}
      >
        <ModalHeader title={modalTitle} />
        <ModalBody>
          <Table aria-label={t("resources")}>
            <Thead>
              <Tr>
                <Th>{t("requestor")}</Th>
                <Th>{t("permissionRequests")}</Th>
                <Th aria-hidden="true"></Th>
              </Tr>
            </Thead>
            <Tbody>
              {resource.shareRequests?.map((shareRequest) => (
                <Tr key={shareRequest.username}>
                  <Td>
                    {shareRequest.firstName} {shareRequest.lastName}{" "}
                    {shareRequest.lastName ? "" : shareRequest.username}
                    <br />
                    <Content component="small">{shareRequest.email}</Content>
                  </Td>
                  <Td>
                    {shareRequest.scopes.map((scope) => (
                      <Label variant="outline" key={scope.toString()}>
                        {scope as string}
                      </Label>
                    ))}
                  </Td>
                  <Td>
                    <Button
                      onClick={async () => {
                        await approveDeny(shareRequest, true);
                      }}
                    >
                      {t("accept")}
                    </Button>
                    <Button
                      onClick={async () => {
                        await approveDeny(shareRequest);
                      }}
                      className="pf-v6-u-ml-sm"
                      variant="danger"
                    >
                      {t("deny")}
                    </Button>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </ModalBody>
        <ModalFooter>
          <Button key="close" variant="link" onClick={toggle}>
            {t("close")}
          </Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
