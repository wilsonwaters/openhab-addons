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
package org.openhab.binding.zabbix.internal.handler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.zabbix.internal.ZabbixServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZabbixServerBridgeHandler} is an OpenHAB bridge which provides access to individual hosts configured
 * in Zabbix. It is responsible for listing what hosts are configured on the server.
 *
 * @author Wilson Waters - Initial contribution 20200212
 */
@NonNullByDefault
public class ZabbixServerBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(ZabbixServerBridgeHandler.class);
    private static final int DEFAULT_REFRESH_PERIOD = 10;
    private final Set<ZabbixBaseThingHandler> services = new HashSet<>();
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable ZabbixServerConfiguration config;

    public ZabbixServerBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    public void registerService(final ZabbixBaseThingHandler service) {
        this.services.add(service);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        final ZabbixServerConfiguration config = getConfigAs(ZabbixServerConfiguration.class);

        boolean validConfig = true;
        String errorMsg = null;
        if (StringUtils.trimToNull(config.hostname) == null) {
            errorMsg = "Parameter 'hostname' is mandatory and must be configured";
            validConfig = false;
        }
        if (config.refreshInterval != null && config.refreshInterval <= 0) {
            errorMsg = "Parameter 'refresh' must be at least 1 second";
            validConfig = false;
        }

        if (validConfig) {
            startAutomaticRefresh(config);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errorMsg);
        }
    }

    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        services.clear();
    }

    /**
     * Start the job refreshing the data
     */
    private void startAutomaticRefresh(ZabbixServerConfiguration config) {
        if (refreshJob == null || refreshJob.isCancelled()) {
            Runnable runnable = () -> {
                boolean online = false;
                try {
                    InetAddress inet;
                    inet = InetAddress.getByName(config.hostname);
                    if (inet.isReachable(5000)) {
                        online = true;
                    }
                } catch (IOException e) {
                    logger.debug("Connection Error: {}", e.getMessage());
                    return;
                }

                if (!online) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                            "hostname or ip is not reachable");
                } else {
                    updateStatus(ThingStatus.ONLINE);
                    for (ZabbixBaseThingHandler service : services) {
                        service.refresh(config);
                    }
                }
            };

            int delay = (config.refreshInterval != null) ? config.refreshInterval.intValue() : DEFAULT_REFRESH_PERIOD;
            refreshJob = scheduler.scheduleWithFixedDelay(runnable, 0, delay, TimeUnit.SECONDS);
        }
    }
}
