import React from "react";
import {
  Button,
  Modal,
  ModalVariant,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";

export const GroupsCreate = () => {
  const { t } = useTranslation("group");

  return (
    <Modal
    variant={ModalVariant.medium}
    title="Medium modal header"
    isOpen={isModalOpen}
    onClose={this.handleModalToggle}
    actions={
      <Button key="confirm" variant="primary" onClick={this.handleModalToggle}>
        Confirm
      </Button>
    }
  >
    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore
    magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
    consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla
    pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id
    est laborum.
  </Modal>
  );
};
