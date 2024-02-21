import type GroupRepresentation from "@keycloak/keycloak-admin-client/lib/defs/groupRepresentation";
import type RealmRepresentation from "@keycloak/keycloak-admin-client/lib/defs/realmRepresentation";
import { UserProfileMetadata } from "@keycloak/keycloak-admin-client/lib/defs/userProfileMetadata";
import type UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import {
  ActionGroup,
  AlertVariant,
  Button,
  Chip,
  ChipGroup,
  FormGroup,
  InputGroup,
  Switch,
} from "@patternfly/react-core";
import { TFunction } from "i18next";
import { useState } from "react";
import { Controller, UseFormReturn } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { HelpItem, UserProfileFields } from "ui-shared";

import { adminClient } from "../admin-client";
import { useAlerts } from "../components/alert/Alerts";
import { FormAccess } from "../components/form/FormAccess";
import { GroupPickerDialog } from "../components/group/GroupPickerDialog";
import { KeycloakTextInput } from "../components/keycloak-text-input/KeycloakTextInput";
import { useAccess } from "../context/access/Access";
import { emailRegexPattern } from "../util";
import useFormatDate from "../utils/useFormatDate";
import { FederatedUserLink } from "./FederatedUserLink";
import { UserFormFields, toUserFormFields } from "./form-state";
import { toUsers } from "./routes/Users";
import { RequiredActionMultiSelect } from "./user-credentials/RequiredActionMultiSelect";

export type BruteForced = {
  isBruteForceProtected?: boolean;
  isLocked?: boolean;
};

export type UserFormProps = {
  form: UseFormReturn<UserFormFields>;
  realm: RealmRepresentation;
  user?: UserRepresentation;
  bruteForce?: BruteForced;
  userProfileMetadata?: UserProfileMetadata;
  save: (user: UserFormFields) => void;
  onGroupsUpdate?: (groups: GroupRepresentation[]) => void;
};

export const UserForm = ({
  form,
  realm,
  user,
  bruteForce: { isBruteForceProtected, isLocked } = {
    isBruteForceProtected: false,
    isLocked: false,
  },
  userProfileMetadata,
  save,
  onGroupsUpdate,
}: UserFormProps) => {
  const { t } = useTranslation();
  const formatDate = useFormatDate();
  const { addAlert, addError } = useAlerts();
  const { hasAccess } = useAccess();
  const isManager = hasAccess("manage-users");
  const canViewFederationLink = hasAccess("view-realm");

  const {
    handleSubmit,
    register,
    setValue,
    watch,
    control,
    reset,
    formState: { errors },
  } = form;
  const watchUsernameInput = watch("username");
  const [selectedGroups, setSelectedGroups] = useState<GroupRepresentation[]>(
    [],
  );
  const [open, setOpen] = useState(false);
  const [locked, setLocked] = useState(isLocked);

  setValue("requiredActions", user?.requiredActions || []);

  const unLockUser = async () => {
    try {
      await adminClient.attackDetection.del({ id: user!.id! });
      addAlert(t("unlockSuccess"), AlertVariant.success);
    } catch (error) {
      addError("unlockError", error);
    }
  };

  const deleteItem = (id: string) => {
    setSelectedGroups(selectedGroups.filter((item) => item.name !== id));
    onGroupsUpdate?.(selectedGroups);
  };

  const addChips = async (groups: GroupRepresentation[]): Promise<void> => {
    setSelectedGroups([...selectedGroups!, ...groups]);
    onGroupsUpdate?.([...selectedGroups!, ...groups]);
  };

  const addGroups = async (groups: GroupRepresentation[]): Promise<void> => {
    const newGroups = groups;

    newGroups.forEach(async (group) => {
      try {
        await adminClient.users.addToGroup({
          id: user!.id!,
          groupId: group.id!,
        });
        addAlert(t("addedGroupMembership"), AlertVariant.success);
      } catch (error) {
        addError("addedGroupMembershipError", error);
      }
    });
  };

  const toggleModal = () => {
    setOpen(!open);
  };

  return (
    <FormAccess
      isHorizontal
      onSubmit={handleSubmit(save)}
      role="query-users"
      fineGrainedAccess={user?.access?.manage}
      className="pf-u-mt-lg"
    >
      {open && (
        <GroupPickerDialog
          type="selectMany"
          text={{
            title: "selectGroups",
            ok: "join",
          }}
          canBrowse={isManager}
          onConfirm={(groups) => {
            user?.id ? addGroups(groups || []) : addChips(groups || []);
            setOpen(false);
          }}
          onClose={() => setOpen(false)}
          filterGroups={selectedGroups}
        />
      )}
      {user?.id && (
        <>
          <FormGroup label={t("id")} fieldId="kc-id" isRequired>
            <KeycloakTextInput
              id={user.id}
              aria-label={t("userID")}
              value={user.id}
              type="text"
              isReadOnly
            />
          </FormGroup>
          <FormGroup label={t("createdAt")} fieldId="kc-created-at" isRequired>
            <KeycloakTextInput
              value={formatDate(new Date(user.createdTimestamp!))}
              type="text"
              id="kc-created-at"
              aria-label={t("createdAt")}
              name="createdTimestamp"
              isReadOnly
            />
          </FormGroup>
        </>
      )}
      <RequiredActionMultiSelect
        control={control}
        name="requiredActions"
        label="requiredUserActions"
        help="requiredUserActionsHelp"
      />
      {(user?.federationLink || user?.origin) && canViewFederationLink && (
        <FormGroup
          label={t("federationLink")}
          labelIcon={
            <HelpItem
              helpText={t("federationLinkHelp")}
              fieldLabelId="federationLink"
            />
          }
        >
          <FederatedUserLink user={user} />
        </FormGroup>
      )}
      {userProfileMetadata ? (
        <>
          <FormGroup
            label={t("emailVerified")}
            fieldId="kc-email-verified"
            helperTextInvalid={t("required")}
            labelIcon={
              <HelpItem
                helpText={t("emailVerifiedHelp")}
                fieldLabelId="emailVerified"
              />
            }
          >
            <Controller
              name="emailVerified"
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  data-testid="email-verified-switch"
                  id="kc-user-email-verified"
                  onChange={(value) => field.onChange(value)}
                  isChecked={field.value}
                  label={t("yes")}
                  labelOff={t("no")}
                />
              )}
            />
          </FormGroup>
          <UserProfileFields
            form={form}
            userProfileMetadata={userProfileMetadata}
            hideReadOnly={!user}
            supportedLocales={realm.supportedLocales || []}
            t={
              ((key: unknown, params) =>
                t(key as string, params as any)) as TFunction
            }
          />
        </>
      ) : (
        <>
          {!realm.registrationEmailAsUsername && (
            <FormGroup
              label={t("username")}
              fieldId="kc-username"
              isRequired
              validated={errors.username ? "error" : "default"}
              helperTextInvalid={t("required")}
            >
              <KeycloakTextInput
                id="kc-username"
                isReadOnly={
                  !!user?.id &&
                  !realm.editUsernameAllowed &&
                  realm.editUsernameAllowed !== undefined
                }
                {...register("username")}
              />
            </FormGroup>
          )}
          <FormGroup
            label={t("email")}
            fieldId="kc-email"
            validated={errors.email ? "error" : "default"}
            helperTextInvalid={t("emailInvalid")}
          >
            <KeycloakTextInput
              type="email"
              id="kc-email"
              data-testid="email-input"
              {...register("email", {
                pattern: emailRegexPattern,
              })}
            />
          </FormGroup>
          <FormGroup
            label={t("emailVerified")}
            fieldId="kc-email-verified"
            helperTextInvalid={t("required")}
            labelIcon={
              <HelpItem
                helpText={t("emailVerifiedHelp")}
                fieldLabelId="emailVerified"
              />
            }
          >
            <Controller
              name="emailVerified"
              defaultValue={false}
              control={control}
              render={({ field }) => (
                <Switch
                  data-testid="email-verified-switch"
                  id="kc-user-email-verified"
                  onChange={(value) => field.onChange(value)}
                  isChecked={field.value}
                  label={t("yes")}
                  labelOff={t("no")}
                />
              )}
            />
          </FormGroup>
          <FormGroup
            label={t("firstName")}
            fieldId="kc-firstName"
            validated={errors.firstName ? "error" : "default"}
            helperTextInvalid={t("required")}
          >
            <KeycloakTextInput
              data-testid="firstName-input"
              id="kc-firstName"
              {...register("firstName")}
            />
          </FormGroup>
          <FormGroup
            label={t("lastName")}
            fieldId="kc-lastName"
            validated={errors.lastName ? "error" : "default"}
          >
            <KeycloakTextInput
              data-testid="lastName-input"
              id="kc-lastName"
              aria-label={t("lastName")}
              {...register("lastName")}
            />
          </FormGroup>
        </>
      )}
      {isBruteForceProtected && (
        <FormGroup
          label={t("temporaryLocked")}
          fieldId="temporaryLocked"
          labelIcon={
            <HelpItem
              helpText={t("temporaryLockedHelp")}
              fieldLabelId="temporaryLocked"
            />
          }
        >
          <Switch
            data-testid="user-locked-switch"
            id="temporaryLocked"
            onChange={(value) => {
              unLockUser();
              setLocked(value);
            }}
            isChecked={locked}
            isDisabled={!locked}
            label={t("on")}
            labelOff={t("off")}
          />
        </FormGroup>
      )}
      {!user?.id && (
        <FormGroup
          label={t("groups")}
          fieldId="kc-groups"
          validated={errors.requiredActions ? "error" : "default"}
          helperTextInvalid={t("required")}
          labelIcon={<HelpItem helpText={t("groups")} fieldLabelId="groups" />}
        >
          <Controller
            name="groups"
            defaultValue={[]}
            control={control}
            render={() => (
              <InputGroup>
                <ChipGroup categoryName={" "}>
                  {selectedGroups.map((currentChip) => (
                    <Chip
                      key={currentChip.id}
                      onClick={() => deleteItem(currentChip.name!)}
                    >
                      {currentChip.path}
                    </Chip>
                  ))}
                </ChipGroup>
                <Button
                  id="kc-join-groups-button"
                  onClick={toggleModal}
                  variant="secondary"
                  data-testid="join-groups-button"
                >
                  {t("joinGroups")}
                </Button>
              </InputGroup>
            )}
          />
        </FormGroup>
      )}

      <ActionGroup>
        <Button
          data-testid={!user?.id ? "create-user" : "save-user"}
          isDisabled={
            !user?.id &&
            !watchUsernameInput &&
            realm.registrationEmailAsUsername === false
          }
          variant="primary"
          type="submit"
        >
          {user?.id ? t("save") : t("create")}
        </Button>
        <Button
          data-testid="cancel-create-user"
          variant="link"
          onClick={user?.id ? () => reset(toUserFormFields(user)) : undefined}
          component={
            !user?.id
              ? (props) => (
                  <Link {...props} to={toUsers({ realm: realm.realm! })} />
                )
              : undefined
          }
        >
          {user?.id ? t("revert") : t("cancel")}
        </Button>
      </ActionGroup>
    </FormAccess>
  );
};
