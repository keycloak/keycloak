import {
  Badge,
  Button,
  DataList,
  DataListContent,
  DataListItem,
  DataListItemRow,
  Grid,
  GridItem,
  Modal,
  ModalVariant,
  Spinner,
  Form,
  FormGroup,
  Split,
  SplitItem,
  TextArea,
  FormHelperText,
  HelperText,
  HelperTextItem,
} from "@patternfly/react-core";
import {
  ExclamationCircleIcon,
  KeyIcon,
  PlusIcon,
} from "@patternfly/react-icons";
import { FC, useEffect, useState } from "react";
import { Trans, useTranslation } from "react-i18next";
import { useAlerts, useEnvironment } from "@keycloak/keycloak-ui-shared";
import { getSSHKeys, updateSHHKeys } from "../api/methods";
import { Page } from "../components/page/Page";
import { usePromise } from "../utils/usePromise";
import type { Environment } from "../environment";

const SSHKeys = () => {
  const { t } = useTranslation();
  const context = useEnvironment<Environment>();
  const { addAlert, addError } = useAlerts();
  const [sshKeys, setSshKeys] = useState<string[]>();
  const [openModal, setOpenModal] = useState(false);
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [deleteSSHKeyIndex, setDeleteSSHKeyIndex] = useState(-1);
  usePromise((signal) => getSSHKeys({ signal, context }), setSshKeys, [key]);

  if (!sshKeys) {
    return <Spinner />;
  }

  const saveSSHKeys = async (sshKeys: string[], type: string) => {
    try {
      await updateSHHKeys(context, sshKeys);
      refresh();
      addAlert(t(type === "add" ? "sshKeyAddSuccess" : "sshKeyDeleteSuccess"));
    } catch (error) {
      addError(t(type === "add" ? "sshKeyAddError" : "sshKeyAddSuccess"));
    }
  };

  return (
    <Page title={t("sshKeys")} description={t("sshKeysDescrption")}>
      <ConfirmationModal
        deleteSSHKeyIndex={deleteSSHKeyIndex}
        close={() => {
          setDeleteSSHKeyIndex(-1);
        }}
        deleteSSHKey={(index: number) => {
          const array = [...sshKeys];
          array.splice(index, 1);
          saveSSHKeys(array, "delete");
          setDeleteSSHKeyIndex(-1);
        }}
      />
      <SSHKeyModal
        openModal={openModal}
        sshKeys={sshKeys}
        close={() => {
          setOpenModal(false);
        }}
        save={(keyValue: string) => {
          sshKeys.push(keyValue);
          saveSSHKeys([...sshKeys], "add");
          setOpenModal(false);
        }}
      />
      <Split hasGutter className="pf-v5-u-mb-lg">
        <SplitItem isFilled></SplitItem>
        <SplitItem>
          <Button
            variant="primary"
            icon={<PlusIcon />}
            iconPosition="right"
            onClick={() => setOpenModal(true)}
          >
            {t("addNew")}
          </Button>
        </SplitItem>
      </Split>
      <DataList className="ssh-keys-list" aria-label={t("sshKeys")}>
        <DataListItem>
          {sshKeys.map((key, index) => (
            <DataListItemRow key={key}>
              <DataListContent
                aria-label="ssh-keys-content"
                className="pf-v5-u-flex-grow-1"
              >
                <Grid hasGutter>
                  <GridItem span={1}>
                    <KeyIcon />
                    <Badge isRead>SSH</Badge>
                  </GridItem>
                  <GridItem sm={8} md={9} span={10}>
                    <span className="pf-u-mr-md">{key}</span>
                  </GridItem>
                  <GridItem
                    className="pf-v5-u-text-align-right"
                    sm={3}
                    md={2}
                    span={1}
                  >
                    <Button
                      variant="danger"
                      onClick={() => {
                        setDeleteSSHKeyIndex(index);
                      }}
                    >
                      Delete
                    </Button>
                  </GridItem>
                </Grid>
              </DataListContent>
            </DataListItemRow>
          ))}
        </DataListItem>
      </DataList>
    </Page>
  );
};

const ConfirmationModal = (props: any) => {
  const { t } = useTranslation();

  return (
    <Modal
      variant={ModalVariant.medium}
      header={
        <h1 className="pf-c-modal-box__title gm_modal-title gm_flex-center">
          {t("sshKeyDeleteConfirmationTitle")}
        </h1>
      }
      isOpen={props.deleteSSHKeyIndex >= 0}
      onClose={() => {
        props.close();
      }}
      actions={[
        <Button
          key="cancel"
          variant="secondary"
          isDanger
          onClick={() => {
            console.log(props.deleteSSHKeyIndex);
            props.deleteSSHKey(props.deleteSSHKeyIndex);
          }}
        >
          {t("sshKeyDeleteConfirmationButton")}
        </Button>,
      ]}
    >
      <Trans i18nKey="sshKeyDeleteConfirmationMessage">
        <strong></strong>
      </Trans>
    </Modal>
  );
};

const SSHKeyModal: FC<any> = (props) => {
  const { t } = useTranslation();
  const [error, setError] = useState("");
  const [keyValue, setKeyValue] = useState("");

  function validateSSHKey(input: string) {
    const regexArray = [
      /^ssh-dss AAAAB3NzaC1kc3[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
      /^ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNT[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
      /^ecdsa-sha2-nistp384 AAAA[0-9A-Za-z+/]+[=]{0,3}( [^\s]+)*$/,
      /^ecdsa-sha2-nistp521 AAAA[0-9A-Za-z+/]+[=]{0,3}( [^\s]+)*$/,
      /^sk-ecdsa-sha2-nistp256@openssh.com AAAAInNrLWVjZHNhLXNoYTItbmlzdHAyNTZAb3BlbnNzaC5jb2[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
      /^ssh-ed25519 AAAAC3NzaC1lZDI1NTE5[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
      /^sk-ssh-ed25519@openssh.com AAAAGnNrLXNzaC1lZDI1NTE5QG9wZW5zc2guY29t[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
      /^ssh-rsa AAAAB3NzaC1yc2[0-9A-Za-z+/]+[=]{0,3}(\s.*)?$/,
    ];
    if (props.sshKeys.includes(input)) {
      return "dublicate";
    }
    for (let i = 0; i < regexArray.length; i++) {
      if (regexArray[i].test(input)) {
        return "";
      }
    }
    return "invalid";
  }

  useEffect(() => {
    setError(validateSSHKey(keyValue));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyValue]);

  const close = () => {
    props.close();
    setKeyValue("");
  };

  return (
    <Modal
      variant={ModalVariant.large}
      header={
        <h1 className="pf-c-modal-box__title gm_modal-title gm_flex-center">
          {t("addNewShhKey")}
        </h1>
      }
      isOpen={props.openModal}
      onClose={() => {
        close();
      }}
      actions={[
        <Button
          key="confirm"
          variant="primary"
          isDisabled={!keyValue || !!error}
          onClick={() => {
            setKeyValue("");
            props.save(keyValue);
          }}
        >
          {t("addShhKey")}
        </Button>,
        <Button
          key="cancel"
          variant="secondary"
          isDanger
          onClick={() => {
            close();
          }}
        >
          {t("cancel")}
        </Button>,
      ]}
    >
      <Form>
        <FormGroup label={"Key"} isRequired fieldId="simple-form-name-01">
          <TextArea
            isRequired
            type="text"
            autoResize={true}
            placeholder={t("sshKeyInputPlaceholder")}
            id="simple-form-name-01"
            name="simple-form-name-01"
            aria-describedby="simple-form-name-01-helper"
            value={keyValue}
            validated={keyValue && error ? "error" : "default"}
            onChange={(_: any, value: string) => {
              setKeyValue(value.trim());
            }}
          />
          {error && keyValue && (
            <FormHelperText>
              <HelperText>
                <HelperTextItem
                  icon={<ExclamationCircleIcon />}
                  variant={error ? "error" : "default"}
                >
                  {t(
                    error === "dublicate"
                      ? "sshKeyErrorDublicate"
                      : "sshKeyErrorInvalid",
                  )}
                </HelperTextItem>
              </HelperText>
            </FormHelperText>
          )}
        </FormGroup>
      </Form>
    </Modal>
  );
};

export default SSHKeys;
