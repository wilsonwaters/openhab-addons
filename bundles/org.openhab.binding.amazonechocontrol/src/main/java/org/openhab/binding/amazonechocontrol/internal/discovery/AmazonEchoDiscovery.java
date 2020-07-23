/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.amazonechocontrol.internal.discovery;

import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.DEVICE_PROPERTY_FAMILY;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.DEVICE_PROPERTY_FLASH_BRIEFING_PROFILE;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.DEVICE_PROPERTY_SERIAL_NUMBER;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.SUPPORTED_ECHO_THING_TYPES_UIDS;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.THING_TYPE_ECHO;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.THING_TYPE_ECHO_SHOW;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.THING_TYPE_ECHO_SPOT;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.THING_TYPE_ECHO_WHA;
import static org.openhab.binding.amazonechocontrol.internal.AmazonEchoControlBindingConstants.THING_TYPE_FLASH_BRIEFING_PROFILE;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.amazonechocontrol.internal.Connection;
import org.openhab.binding.amazonechocontrol.internal.handler.AccountHandler;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonDevices.Device;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AmazonEchoDiscovery} is responsible for discovering echo devices on
 * the amazon account specified in the binding.
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class AmazonEchoDiscovery extends AbstractDiscoveryService  {

    AccountHandler accountHandler;
    private final Logger logger = LoggerFactory.getLogger(AmazonEchoDiscovery.class);
    private final Set<String> discoveredFlashBriefings = new HashSet<>();

    private @Nullable ScheduledFuture<?> startScanStateJob;
    private @Nullable Long activateTimeStamp;

    public AmazonEchoDiscovery(AccountHandler accountHandler) {
        super(SUPPORTED_ECHO_THING_TYPES_UIDS, 10);
        this.accountHandler = accountHandler;
    }

    public void activate() {
        activate(new HashMap<>());
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
        stopScanJob();
        final Long activateTimeStamp = this.activateTimeStamp;
        if (activateTimeStamp != null) {
            removeOlderResults(activateTimeStamp);
        }
        setDevices(accountHandler.updateDeviceList());

        String currentFlashBriefingConfiguration = accountHandler.getNewCurrentFlashbriefingConfiguration();
        discoverFlashBriefingProfiles(currentFlashBriefingConfiguration);
    }

    protected void startAutomaticScan() {
        if (!this.accountHandler.getThing().getThings().isEmpty()) {
            stopScanJob();
            return;
        }
        Connection connection = this.accountHandler.findConnection();
        if (connection == null) {
            return;
        }
        Date verifyTime = connection.tryGetVerifyTime();
        if (verifyTime == null) {
            return;
        }
        if (new Date().getTime() - verifyTime.getTime() < 10000) {
            return;
        }
        startScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        stopScanJob();
        startScanStateJob = scheduler.scheduleWithFixedDelay(this::startAutomaticScan, 3000, 1000,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        stopScanJob();
    }

    void stopScanJob() {
        @Nullable
        ScheduledFuture<?> currentStartScanStateJob = startScanStateJob;
        if (currentStartScanStateJob != null) {
            currentStartScanStateJob.cancel(false);
            startScanStateJob = null;
        }
    }

    @Override
    @Activate
    public void activate(@Nullable Map<String, @Nullable Object> config) {
        super.activate(config);
        if (config != null) {
            modified(config);
        }
        if (activateTimeStamp == null) {
            activateTimeStamp = new Date().getTime();
        }
    }

    synchronized void setDevices(List<Device> deviceList) {
        for (Device device : deviceList) {
            String serialNumber = device.serialNumber;
            if (serialNumber != null) {
                String deviceFamily = device.deviceFamily;
                if (deviceFamily != null) {
                    ThingTypeUID thingTypeId;
                    if (deviceFamily.equals("ECHO")) {
                        thingTypeId = THING_TYPE_ECHO;
                    } else if (deviceFamily.equals("ROOK")) {
                        thingTypeId = THING_TYPE_ECHO_SPOT;
                    } else if (deviceFamily.equals("KNIGHT")) {
                        thingTypeId = THING_TYPE_ECHO_SHOW;
                    } else if (deviceFamily.equals("WHA")) {
                        thingTypeId = THING_TYPE_ECHO_WHA;
                    } else {
                        logger.debug("Unknown thing type '{}'", deviceFamily);
                        continue;
                    }

                    ThingUID brigdeThingUID = this.accountHandler.getThing().getUID();
                    ThingUID thingUID = new ThingUID(thingTypeId, brigdeThingUID, serialNumber);

                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel(device.accountName)
                            .withProperty(DEVICE_PROPERTY_SERIAL_NUMBER, serialNumber)
                            .withProperty(DEVICE_PROPERTY_FAMILY, deviceFamily)
                            .withRepresentationProperty(DEVICE_PROPERTY_SERIAL_NUMBER).withBridge(brigdeThingUID)
                            .build();

                    logger.debug("Device [{}: {}] found. Mapped to thing type {}", device.deviceFamily, serialNumber,
                            thingTypeId.getAsString());

                    thingDiscovered(result);
                }
            }
        }
    }

    public synchronized void discoverFlashBriefingProfiles(String currentFlashBriefingJson) {
        if (currentFlashBriefingJson.isEmpty()) {
            return;
        }

        if (!discoveredFlashBriefings.contains(currentFlashBriefingJson)) {
            ThingUID brigdeThingUID = this.accountHandler.getThing().getUID();
            ThingUID freeThingUID =  new ThingUID(THING_TYPE_FLASH_BRIEFING_PROFILE, brigdeThingUID, Integer.toString(currentFlashBriefingJson.hashCode()));
            DiscoveryResult result = DiscoveryResultBuilder.create(freeThingUID).withLabel("FlashBriefing")
                    .withProperty(DEVICE_PROPERTY_FLASH_BRIEFING_PROFILE, currentFlashBriefingJson)
                    .withBridge(accountHandler.getThing().getUID()).build();
            logger.debug("Flash Briefing {} discovered", currentFlashBriefingJson);
            thingDiscovered(result);
            discoveredFlashBriefings.add(currentFlashBriefingJson);
        }
    }

    public synchronized void removeExistingFlashBriefingProfile(@Nullable String currentFlashBriefingJson) {
        if (currentFlashBriefingJson != null) {
            discoveredFlashBriefings.remove(currentFlashBriefingJson);
        }
    }
}
