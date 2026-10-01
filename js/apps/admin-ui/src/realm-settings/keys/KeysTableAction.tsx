import {
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  Button,
  ButtonVariant,
  Dropdown,
  DropdownList,
  MenuToggle,
  OverflowMenuControl,
  OverflowMenuDropdownItem,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import { useConfirmDialog } from "../../components/confirm-dialog/ConfirmDialog";
import { RhUiEllipsisVerticalFillIcon } from "@patternfly/react-icons";
import useToggle from "../../utils/useToggle";

type KeysTableActionProps = {
  publicKey?: string;
  certificate?: string;
};

export const KeysTableAction = ({
  publicKey,
  certificate,
}: KeysTableActionProps) => {
  const { t } = useTranslation();
  const [open, toggle] = useToggle();

  const [togglePublicKeyDialog, PublicKeyDialog] = useConfirmDialog({
    titleKey: t("publicKey"),
    messageKey: publicKey,
    continueButtonLabel: "close",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: () => Promise.resolve(),
  });

  const [toggleCertificateDialog, CertificateDialog] = useConfirmDialog({
    titleKey: t("certificate"),
    messageKey: certificate,
    continueButtonLabel: "close",
    continueButtonVariant: ButtonVariant.primary,
    onConfirm: () => Promise.resolve(),
  });
  return (
    <>
      <PublicKeyDialog />
      <CertificateDialog />
      <OverflowMenu breakpoint={certificate ? "lg" : "md"}>
        <OverflowMenuContent>
          <OverflowMenuGroup groupType="button">
            {publicKey && (
              <OverflowMenuItem>
                <Button
                  onClick={() => {
                    togglePublicKeyDialog();
                  }}
                  variant="secondary"
                  id={publicKey}
                >
                  {t("publicKey")}
                </Button>
              </OverflowMenuItem>
            )}
            {certificate && (
              <OverflowMenuItem>
                <Button
                  onClick={() => {
                    toggleCertificateDialog();
                  }}
                  variant="secondary"
                  id={certificate}
                  className="kc-certificate"
                >
                  {t("certificate")}
                </Button>
              </OverflowMenuItem>
            )}
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl>
          <Dropdown
            onSelect={(_, v) => {
              switch (v) {
                case 0:
                  togglePublicKeyDialog();
                  break;
                case 1:
                  toggleCertificateDialog();
                  break;
              }
              toggle();
            }}
            toggle={(toggleRef) => (
              <MenuToggle
                ref={toggleRef}
                variant="plain"
                aria-label="Table actions overflow menu"
                onClick={toggle}
                isExpanded={open}
                icon={<RhUiEllipsisVerticalFillIcon />}
              />
            )}
            isOpen={open}
            onOpenChange={(isOpen) => (isOpen ? toggle() : undefined)}
          >
            <DropdownList>
              <OverflowMenuDropdownItem itemId={0} isShared>
                {t("publicKey")}
              </OverflowMenuDropdownItem>
              <OverflowMenuDropdownItem itemId={1} isShared>
                {t("certificate")}
              </OverflowMenuDropdownItem>
            </DropdownList>
          </Dropdown>
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
