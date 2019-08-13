/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.device;

import org.keycloak.models.DeviceModel;
import org.keycloak.models.UserDeviceStore;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.Provider;

/**
 * <p>A {@link DeviceFingerPrintProvider} is responsible for providing the necessary logic to fingerprint devices so that
 * they can be associated with user sessions and track the device activity of users.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface DeviceFingerPrintProvider extends Provider {

    /**
     * <p>Returns the current device. Implementations can map the current device in different ways such as using the {@code User-Agent} HTTP Header when the user is authenticating using a browser.
     *
     * <p>If the device is already associated with the {@link UserSessionModel} it can be obtained as a note with name {@link DeviceModel#DEVICE_ID}.
     *
     * <p>If the device is not associated with the {@link UserSessionModel} but the implementation is capable of mapping a stored device from requests (e.g.: by using a specific header or cookie), this method can return the stored device by using a {@link org.keycloak.models.UserDeviceStore}.
     *
     * @param userSession the user session
     * @param deviceStore the device store
     * @return the current device or null if no device could be mapped from the current request
     */
    DeviceModel getCurrentDevice(UserSessionModel userSession, UserDeviceStore deviceStore);

    /**
     * Creates a server-side fingerprint so that a device can be identified on subsequent requests. This method is specially useful
     * when the server is capable of sending back information to a device so that it can be stored by the device and sent on subsequent requests.
     *
     * @param userSession the user session
     */
    void createFingerprint(UserSessionModel userSession);

    /**
     * Compares if the {@code current} device (usually returned by the {@link #getCurrentDevice(UserSessionModel, UserDeviceStore)} method) is the same if compared to another {@code device}.
     *
     * @param current the current device
     * @param device the device to compare
     * @return true if the devices are the same. Otherwise, false.
     */
    boolean isSameDevice(DeviceModel current, DeviceModel device);
}
