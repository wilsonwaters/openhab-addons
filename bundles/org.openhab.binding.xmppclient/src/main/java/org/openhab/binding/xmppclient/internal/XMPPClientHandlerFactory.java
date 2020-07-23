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
package org.openhab.binding.xmppclient.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.xmppclient.XMPPClientBindingConstants;
import org.openhab.binding.xmppclient.handler.XMPPClientHandler;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link XMPPClientHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Pavel Gololobov - Initial contribution
 */
@Component(configurationPid = "binding.xmppclient", service = ThingHandlerFactory.class)
public class XMPPClientHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(XMPPClientBindingConstants.BRIDGE_TYPE_XMPP);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(XMPPClientBindingConstants.BRIDGE_TYPE_XMPP)) {
            return new XMPPClientHandler((Bridge) thing);
        }
        return null;
    }
}
