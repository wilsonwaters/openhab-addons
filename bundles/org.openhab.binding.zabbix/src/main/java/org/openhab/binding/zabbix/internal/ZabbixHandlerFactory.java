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
package org.openhab.binding.zabbix.internal;

import static org.openhab.binding.zabbix.internal.ZabbixBindingConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.zabbix.internal.discovery.ZabbixHostDiscoveryService;
import org.openhab.binding.zabbix.internal.discovery.ZabbixItemDiscoveryService;
import org.openhab.binding.zabbix.internal.handler.ZabbixHostHandler;
import org.openhab.binding.zabbix.internal.handler.ZabbixServerBridgeHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZabbixHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Wilson Waters
 *         Initial addition 20200212
 */
@NonNullByDefault
@Component(configurationPid = "binding.zabbix", service = ThingHandlerFactory.class)
public class ZabbixHandlerFactory extends BaseThingHandlerFactory {
    Logger logger = LoggerFactory.getLogger(ZabbixHandlerFactory.class);
    private final Map<ThingUID, @Nullable ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>() {

        private static final long serialVersionUID = 1L;
        {
            add(THING_TYPE_SERVER_BRIDGE);
            add(THING_TYPE_HOST);
        }
    };

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_SERVER_BRIDGE)) {
            ZabbixServerBridgeHandler zabbixServerBridgeHandler = new ZabbixServerBridgeHandler((Bridge) thing);
            registerDiscoveryService(zabbixServerBridgeHandler); // discover hosts associaged with this server
            return zabbixServerBridgeHandler;
        } else if (thingTypeUID.equals(THING_TYPE_HOST)) {
            ZabbixHostHandler zabbixHostHandler = new ZabbixHostHandler(thing);
            registerDiscoveryService(zabbixHostHandler); // discover channels associated with this host
            return zabbixHostHandler;
        }
        return null;
    }

    private synchronized void registerDiscoveryService(ZabbixServerBridgeHandler zabbixServerBridgeHandler) {
        ZabbixHostDiscoveryService zabbixHostDiscoveryService = new ZabbixHostDiscoveryService(
                zabbixServerBridgeHandler);

        this.discoveryServiceRegs.put(zabbixServerBridgeHandler.getThing().getUID(), bundleContext.registerService(
                DiscoveryService.class.getName(), zabbixHostDiscoveryService, new Hashtable<String, Object>()));
    }

    private synchronized void registerDiscoveryService(ZabbixHostHandler zabbixHostHandler) {
        ZabbixItemDiscoveryService zabbixItemDiscoveryService = new ZabbixItemDiscoveryService(zabbixHostHandler);

        this.discoveryServiceRegs.put(zabbixHostHandler.getThing().getUID(), bundleContext.registerService(
                DiscoveryService.class.getName(), zabbixItemDiscoveryService, new Hashtable<String, Object>()));
    }
}
