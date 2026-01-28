import { useState, useEffect } from "react";

/** A 'plain' object version of the permission status. */
export type PlainPermissionStatus = {
  readonly name: string;
  readonly state: PermissionState;
};

export default function useQueryPermission(
  name: PermissionName,
): PlainPermissionStatus | null {
  const [status, setStatus] = useState<PermissionStatus | null>(null);
  const [plainStatus, setPlainStatus] = useState<PlainPermissionStatus | null>(
    null,
  );

  function updatePlainStatus(newStatus: PermissionStatus) {
    setPlainStatus({
      name: newStatus.name,
      state: newStatus.state,
    });
  }

  // Query the permission status when the name changes.
  useEffect(() => {
    setStatus(null);
    setPlainStatus(null);

    void navigator.permissions.query({ name }).then((newStatus) => {
      setStatus(newStatus);
      updatePlainStatus(newStatus);
    });
  }, [name]);

  // Update the 'plain' status when the permission status changes.
  useEffect(() => {
    if (!status) {
      return;
    }

    function onStatusChange() {
      if (!status) {
        return;
      }

      updatePlainStatus(status);
    }

    status.addEventListener("change", onStatusChange);
    return () => status.removeEventListener("change", onStatusChange);
  }, [status]);

  return plainStatus;
}
