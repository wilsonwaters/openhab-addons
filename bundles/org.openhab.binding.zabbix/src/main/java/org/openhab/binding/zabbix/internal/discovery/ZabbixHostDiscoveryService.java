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
package org.openhab.binding.zabbix.internal.discovery;

import static org.openhab.binding.zabbix.internal.ZabbixBindingConstants.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zabbix.internal.api.ZabbixAPIException;
import org.openhab.binding.zabbix.internal.api.ZabbixAPIHostObject;
import org.openhab.binding.zabbix.internal.api.ZabbixCommunicationException;
import org.openhab.binding.zabbix.internal.handler.ZabbixServerBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZabbixHostDiscoveryService} implements the discovery service for the Zabbix binding.
 * When a Zabbix Server (bridge) is created, this discovery service finds hosts which are configured on
 * the Zabbix server and creates a new ZabbixHost thing.
 *
 * @author Wilson Waters - Initial contribution 20200215
 */
@NonNullByDefault
public class ZabbixHostDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(ZabbixHostDiscoveryService.class);

    private final ZabbixServerBridgeHandler zabbixServerBridgeHandler;

    @Nullable
    ThingUID bridgeUID;

    public ZabbixHostDiscoveryService(ZabbixServerBridgeHandler zabbixServerBridgeHandler) {
        super(Collections.singleton(THING_TYPE_HOST), 60, false);
        this.zabbixServerBridgeHandler = zabbixServerBridgeHandler;
        logger.debug("registering discovery service for {}", zabbixServerBridgeHandler);
    }

    @Override
    public void startScan() {
        bridgeUID = zabbixServerBridgeHandler.getThing().getUID();

        List<ZabbixAPIHostObject> hosts = null;
        try {
            hosts = zabbixServerBridgeHandler.getApi().hostGet();
        } catch (ZabbixCommunicationException e) {
            logger.error("Error connecting to Zabbix server", e);
            return;
        } catch (ZabbixAPIException e) {
            logger.error("Zabbix server gave an error", e);
            return; // or should I throw a RuntimeException?
        }

        for (ZabbixAPIHostObject host : hosts) {
            ThingTypeUID thingTypeUID = THING_TYPE_HOST;
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, host.hostid);
            String hostLabel = "Zabbix host (" + host.host + ")";

            Map<String, Object> properties = new HashMap<>();
            properties.put(HOST_PROPERTY_HOSTNAME, host.host);
            properties.put(HOST_PROPERTY_BRIDGE, zabbixServerBridgeHandler);

            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                    .withProperties(properties).withBridge(bridgeUID).withLabel(hostLabel).build();

            thingDiscovered(discoveryResult);
        }
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
    }

    @Override
    public void deactivate() {
        removeOlderResults(new Date().getTime());
    }

}
