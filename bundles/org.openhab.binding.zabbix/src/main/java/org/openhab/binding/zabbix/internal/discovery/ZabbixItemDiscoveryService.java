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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.zabbix.internal.ZabbixBindingConstants;
import org.openhab.binding.zabbix.internal.api.ZabbixAPIException;
import org.openhab.binding.zabbix.internal.api.ZabbixAPIItemObject;
import org.openhab.binding.zabbix.internal.api.ZabbixCommunicationException;
import org.openhab.binding.zabbix.internal.handler.ZabbixHostHandler;
import org.openhab.binding.zabbix.internal.handler.ZabbixServerBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZabbixItemDiscoveryService} implements the discovery service for the Zabbix binding.
 * When a Zabbix Host (thing) is created, this discovery service finds items which are configured
 * for the host and creates a new channel for the host
 *
 * @author Wilson Waters - Initial contribution 20200215
 */
@NonNullByDefault
public class ZabbixItemDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(ZabbixItemDiscoveryService.class);

    private final ZabbixHostHandler zabbixHostHandler;

    @Nullable
    ThingUID hostThingUID;

    public ZabbixItemDiscoveryService(ZabbixHostHandler zabbixHostHandler) {
        super(Collections.singleton(THING_TYPE_HOST), 60, false);
        this.zabbixHostHandler = zabbixHostHandler;
        logger.debug("registering discovery service for {}", zabbixHostHandler);
    }

    @Override
    public void startScan() {
        hostThingUID = zabbixHostHandler.getThing().getUID();

        List<ZabbixAPIItemObject> items = null;
        try {
            ZabbixServerBridgeHandler serverBridge = zabbixHostHandler.getZabbixServerBridgeHandler();
            items = serverBridge.getApi().itemGet(Arrays.asList(
                    zabbixHostHandler.getThing().getProperties().get(ZabbixBindingConstants.HOST_PROPERTY_HOSTNAME)));
        } catch (ZabbixCommunicationException e) {
            logger.error("Error connecting to Zabbix server", e);
            return;
        } catch (ZabbixAPIException e) {
            logger.error("Zabbix server gave an error", e);
            return; // or should I throw a RuntimeException?
        }

        for (ZabbixAPIItemObject item : items) {
            // String bla = item.key_;
            ThingTypeUID thingTypeUID = THING_TYPE_ITEM;
            // ThingUID thingUID = new ThingUID(thingTypeUID, hostThingUID, item.key_);
            // String hostLabel = "Zabbix item (name=" + item.name)";

            Map<String, Object> properties = new HashMap<>();
            properties.put(ITEM_PROPERTY_ITEMID, item.itemid);
            properties.put(ITEM_PROPERTY_NAME, item.name);
            properties.put(ITEM_PROPERTY_KEY, item.key_);

            // DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
            // .withProperties(properties).withBridge(hostThingUID).withLabel(hostLabel).build();

            // thingDiscovered(discoveryResult);
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
