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
package org.openhab.binding.hdpowerview.internal.discovery;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.hdpowerview.internal.HDPowerViewBindingConstants;
import org.openhab.binding.hdpowerview.internal.HDPowerViewWebTargets;
import org.openhab.binding.hdpowerview.internal.api.responses.Shades;
import org.openhab.binding.hdpowerview.internal.api.responses.Shades.Shade;
import org.openhab.binding.hdpowerview.internal.config.HDPowerViewShadeConfiguration;
import org.openhab.binding.hdpowerview.internal.handler.HDPowerViewHubHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers an HD Power View Shade from an existing hub
 *
 * @author Andy Lintner - Initial contribution
 */
public class HDPowerViewShadeDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(HDPowerViewShadeDiscoveryService.class);
    private final HDPowerViewHubHandler hub;
    private final Runnable scanner;
    private ScheduledFuture<?> backgroundFuture;

    public HDPowerViewShadeDiscoveryService(HDPowerViewHubHandler hub) {
        super(Collections.singleton(HDPowerViewBindingConstants.THING_TYPE_SHADE), 600, true);
        this.hub = hub;
        this.scanner = createScanner();
    }

    @Override
    protected void startScan() {
        scheduler.execute(scanner);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (backgroundFuture != null && !backgroundFuture.isDone()) {
            backgroundFuture.cancel(true);
            backgroundFuture = null;
        }
        backgroundFuture = scheduler.scheduleWithFixedDelay(scanner, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        if (backgroundFuture != null && !backgroundFuture.isDone()) {
            backgroundFuture.cancel(true);
            backgroundFuture = null;
        }
        super.stopBackgroundDiscovery();
    }

    private Runnable createScanner() {
        return () -> {
            HDPowerViewWebTargets targets = hub.getWebTargets();
            Shades shades;
            try {
                shades = targets.getShades();
            } catch (IOException e) {
                logger.warn("Unexpected error: {}", e.getMessage());
                stopScan();
                return;
            }
            if (shades != null) {
                for (Shade shade : shades.shadeData) {
                    ThingUID thingUID = new ThingUID(HDPowerViewBindingConstants.THING_TYPE_SHADE, shade.id);
                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID)
                            .withProperty(HDPowerViewShadeConfiguration.ID, shade.id).withLabel(shade.getName())
                            .withBridge(hub.getThing().getUID()).build();
                    thingDiscovered(result);
                }
            }
            stopScan();
        };
    }
}
