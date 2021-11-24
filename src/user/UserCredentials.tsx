import React, { FunctionComponent, useMemo, useState } from "react";
import {
  AlertVariant,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownItem,
  DropdownPosition,
  Form,
  FormGroup,
  KebabToggle,
  Modal,
  ModalVariant,
  Switch,
  Text,
  TextVariants,
  ValidatedOptions,
} from "@patternfly/react-core";
import {
  Table,
  TableBody,
  TableComposable,
  TableHeader,
  TableVariant,
  Tbody,
  Td,
  Th,
  Thead,
  Tr,
} from "@patternfly/react-table";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import { useTranslation } from "react-i18next";
import { useAlerts } from "../components/alert/Alerts";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { useAdminClient, useFetch } from "../context/auth/AdminClient";
import { useWhoAmI } from "../context/whoami/WhoAmI";
import { Controller, useForm, useWatch } from "react-hook-form";
import { PasswordInput } from "../components/password-input/PasswordInput";
import { HelpItem } from "../components/help-enabler/HelpItem";
import "./user-section.css";
import { useConfirmDialog } from "../components/confirm-dialog/ConfirmDialog";
import type CredentialRepresentation from "@keycloak/keycloak-admin-client/lib/defs/credentialRepresentation";

type UserCredentialsProps = {
  user: UserRepresentation;
};

type CredentialsForm = {
  password: string;
  passwordConfirmation: string;
  temporaryPassword: boolean;
};

const defaultValues: CredentialsForm = {
  password: "",
  passwordConfirmation: "",
  temporaryPassword: true,
};

type DisplayDialogProps = {
  titleKey: string;
  onClose: () => void;
};

const DisplayDialog: FunctionComponent<DisplayDialogProps> = ({
  titleKey,
  onClose,
  children,
}) => {
  const { t } = useTranslation("users");
  return (
    <Modal
      variant={ModalVariant.medium}
      title={t(titleKey)}
      isOpen={true}
      onClose={onClose}
    >
      {children}
    </Modal>
  );
};

export const UserCredentials = ({ user }: UserCredentialsProps) => {
  const { t } = useTranslation("users");
  const { whoAmI } = useWhoAmI();
  const { addAlert, addError } = useAlerts();
  const [key, setKey] = useState(0);
  const refresh = () => setKey(key + 1);
  const [open, setOpen] = useState(false);
  const [openSaveConfirm, setOpenSaveConfirm] = useState(false);
  const [kebabOpen, setKebabOpen] = useState(false);
  const adminClient = useAdminClient();
  const form = useForm<CredentialsForm>({ defaultValues });
  const { control, errors, handleSubmit, register } = form;
  const [credentials, setCredentials] = useState<CredentialsForm>();
  const [userCredentials, setUserCredentials] = useState<
    CredentialRepresentation[]
  >([]);
  const [selectedCredential, setSelectedCredential] =
    useState<CredentialRepresentation>({});
  const [isResetPassword, setIsResetPassword] = useState(false);
  const [showData, setShowData] = useState(false);

  useFetch(
    () => adminClient.users.getCredentials({ id: user.id! }),
    (credentials) => {
      setUserCredentials(credentials);
    },
    [key]
  );

  const passwordWatcher = useWatch<CredentialsForm["password"]>({
    control,
    name: "password",
  });

  const passwordConfirmationWatcher = useWatch<
    CredentialsForm["passwordConfirmation"]
  >({
    control,
    name: "passwordConfirmation",
  });

  const isNotDisabled =
    passwordWatcher !== "" && passwordConfirmationWatcher !== "";

  const toggleModal = () => {
    setOpen(!open);
  };

  const toggleConfirmSaveModal = () => {
    setOpenSaveConfirm(!openSaveConfirm);
  };

  const saveUserPassword = async () => {
    if (!credentials) {
      return;
    }

    const passwordsMatch =
      credentials.password === credentials.passwordConfirmation;

    if (!passwordsMatch) {
      addAlert(
        isResetPassword
          ? t("resetPasswordNotMatchError")
          : t("savePasswordNotMatchError"),
        AlertVariant.danger
      );
    } else {
      try {
        await adminClient.users.resetPassword({
          id: user.id!,
          credential: {
            temporary: credentials.temporaryPassword,
            type: "password",
            value: credentials.password,
          },
        });
        refresh();
        addAlert(
          isResetPassword
            ? t("resetCredentialsSuccess")
            : t("savePasswordSuccess"),
          AlertVariant.success
        );
        setIsResetPassword(false);
        setOpenSaveConfirm(false);
      } catch (error) {
        addError(
          isResetPassword ? t("resetPasswordError") : t("savePasswordError"),
          error
        );
      }
    }
  };

  const resetPassword = () => {
    setIsResetPassword(true);
    setOpen(true);
  };

  const [toggleDeleteDialog, DeleteConfirm] = useConfirmDialog({
    titleKey: t("deleteCredentialsConfirmTitle"),
    messageKey: t("deleteCredentialsConfirm"),
    continueButtonLabel: t("common:delete"),
    continueButtonVariant: ButtonVariant.danger,
    onConfirm: async () => {
      try {
        await adminClient.users.deleteCredential({
          id: user.id!,
          credentialId: selectedCredential.id!,
        });
        addAlert(t("deleteCredentialsSuccess"), AlertVariant.success);
        setKey((key) => key + 1);
      } catch (error) {
        addError(t("deleteCredentialsError"), error);
      }
    },
  });

  const rows = useMemo(() => {
    if (!selectedCredential.credentialData) {
      return [];
    }

    const credentialData = JSON.parse(selectedCredential.credentialData);
    const locale = whoAmI.getLocale();

    return Object.entries(credentialData)
      .sort(([a], [b]) => a.localeCompare(b, locale))
      .map<[string, string]>(([key, value]) => {
        if (typeof value === "string") {
          return [key, value];
        }

        return [key, JSON.stringify(value)];
      });
  }, [selectedCredential.credentialData]);

  return (
    <>
      {open && (
        <Modal
          variant={ModalVariant.small}
          width={600}
          title={
            isResetPassword
              ? `${t("resetPasswordFor")} ${user.username}`
              : `${t("setPasswordFor")} ${user.username}`
          }
          isOpen
          onClose={() => {
            setIsResetPassword(false);
            setOpen(false);
          }}
          actions={[
            <Button
              data-testid="okBtn"
              key={`confirmBtn-${user.id}`}
              variant="primary"
              form="userCredentials-form"
              onClick={() => {
                setOpen(false);
                setCredentials(form.getValues());
                toggleConfirmSaveModal();
              }}
              isDisabled={!isNotDisabled}
            >
              {t("save")}
            </Button>,
            <Button
              data-testid="cancelBtn"
              key={`cancelBtn-${user.id}`}
              variant="link"
              form="userCredentials-form"
              onClick={() => {
                setIsResetPassword(false);
                setOpen(false);
              }}
            >
              {t("cancel")}
            </Button>,
          ]}
        >
          <Form id="userCredentials-form" isHorizontal>
            <FormGroup
              name="password"
              label={t("password")}
              fieldId="password"
              helperTextInvalid={t("common:required")}
              validated={
                errors.password
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isRequired
            >
              <div className="kc-password">
                <PasswordInput
                  name="password"
                  aria-label="password"
                  ref={register({ required: true })}
                />
              </div>
            </FormGroup>
            <FormGroup
              name="passwordConfirmation"
              label={
                isResetPassword
                  ? t("resetPasswordConfirmation")
                  : t("passwordConfirmation")
              }
              fieldId="passwordConfirmation"
              helperTextInvalid={t("common:required")}
              validated={
                errors.passwordConfirmation
                  ? ValidatedOptions.error
                  : ValidatedOptions.default
              }
              isRequired
            >
              <div className="kc-passwordConfirmation">
                <PasswordInput
                  name="passwordConfirmation"
                  aria-label="passwordConfirm"
                  ref={register({ required: true })}
                />
              </div>
            </FormGroup>
            <FormGroup
              label={t("common:temporaryPassword")}
              labelIcon={
                <HelpItem
                  helpText={t("common:temporaryPasswordHelpText")}
                  forLabel={t("common:temporaryPassword")}
                  forID="kc-temporaryPasswordSwitch"
                />
              }
              fieldId="kc-temporaryPassword"
            >
              {" "}
              <Controller
                name="temporaryPassword"
                defaultValue={true}
                control={control}
                render={({ onChange, value }) => (
                  <Switch
                    className={"kc-temporaryPassword"}
                    onChange={(value) => onChange(value)}
                    isChecked={value}
                    label={t("common:on")}
                    labelOff={t("common:off")}
                  />
                )}
              ></Controller>
            </FormGroup>
          </Form>
        </Modal>
      )}
      {openSaveConfirm && (
        <Modal
          variant={ModalVariant.small}
          width={600}
          title={
            isResetPassword
              ? t("resetPasswordConfirm")
              : t("setPasswordConfirm")
          }
          isOpen
          onClose={() => setOpenSaveConfirm(false)}
          actions={[
            <Button
              data-testid="setPasswordBtn"
              key={`confirmSaveBtn-${user.id}`}
              variant="danger"
              form="userCredentials-form"
              onClick={() => {
                handleSubmit(saveUserPassword)();
              }}
            >
              {isResetPassword ? t("resetPassword") : t("savePassword")}
            </Button>,
            <Button
              data-testid="cancelSetPasswordBtn"
              key={`cancelConfirmBtn-${user.id}`}
              variant="link"
              form="userCredentials-form"
              onClick={() => {
                setOpenSaveConfirm(false);
              }}
            >
              {t("cancel")}
            </Button>,
          ]}
        >
          <Text component={TextVariants.h3}>
            {isResetPassword
              ? `${t("resetPasswordConfirmText")} ${user.username} ${t(
                  "questionMark"
                )}`
              : `${t("setPasswordConfirmText")} ${user.username} ${t(
                  "questionMark"
                )}`}
          </Text>
        </Modal>
      )}
      <DeleteConfirm />
      {showData && Object.keys(selectedCredential).length !== 0 && (
        <DisplayDialog
          titleKey={t("passwordDataTitle")}
          onClose={() => {
            setShowData(false);
            setSelectedCredential({});
          }}
        >
          <Table
            aria-label="password-data"
            data-testid="password-data-dialog"
            variant={TableVariant.compact}
            cells={[t("showPasswordDataName"), t("showPasswordDataValue")]}
            rows={rows}
          >
            <TableHeader />
            <TableBody />
          </Table>
        </DisplayDialog>
      )}
      {userCredentials.length !== 0 ? (
        <TableComposable aria-label="password-data-table" variant={"compact"}>
          <Thead>
            <Tr>
              <Th>
                <HelpItem
                  helpText={t("userCredentialsHelpText")}
                  forLabel={t("userCredentialsHelpTextLabel")}
                  forID={t(`common:helpLabel`, {
                    label: t("userCredentialsHelpTextLabel"),
                  })}
                />
              </Th>
              <Th>{t("type")}</Th>
              <Th>{t("userLabel")}</Th>
              <Th>{t("data")}</Th>
              <Th />
              <Th />
            </Tr>
          </Thead>
          <Tbody>
            <Tr key={"key"}>
              {userCredentials.map((credential) => (
                <>
                  <Td
                    draggableRow={{
                      id: `draggable-row-${credential.id}`,
                    }}
                  />
                  <Td key={`${credential}`} dataLabel={`columns-${credential}`}>
                    {credential.type?.charAt(0).toUpperCase()! +
                      credential.type?.slice(1)}
                  </Td>
                  <Td>My Password</Td>
                  <Td>
                    <Button
                      className="kc-showData-btn"
                      variant="link"
                      data-testid="showDataBtn"
                      onClick={() => {
                        setShowData(true);
                        setSelectedCredential(credential);
                      }}
                    >
                      {t("showDataBtn")}
                    </Button>
                  </Td>
                  <Td>
                    <Button
                      variant="secondary"
                      data-testid="resetPasswordBtn"
                      onClick={resetPassword}
                    >
                      {t("resetPasswordBtn")}
                    </Button>
                  </Td>
                  <Td>
                    <Dropdown
                      isPlain
                      position={DropdownPosition.right}
                      toggle={
                        <KebabToggle onToggle={(open) => setKebabOpen(open)} />
                      }
                      isOpen={kebabOpen}
                      onSelect={() => setSelectedCredential(credential)}
                      dropdownItems={[
                        <DropdownItem
                          key={`delete-dropdown-item-${credential.id}`}
                          data-testid="deleteDropdownItem"
                          component="button"
                          onClick={() => {
                            toggleDeleteDialog();
                            setKebabOpen(false);
                          }}
                        >
                          {t("deleteBtn")}
                        </DropdownItem>,
                      ]}
                    />
                  </Td>
                </>
              ))}
            </Tr>
          </Tbody>
        </TableComposable>
      ) : (
        <ListEmptyState
          hasIcon={true}
          message={t("noCredentials")}
          instructions={t("noCredentialsText")}
          primaryActionText={t("setPassword")}
          onPrimaryAction={toggleModal}
        />
      )}
    </>
  );
};
