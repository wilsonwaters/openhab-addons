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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ZabbixBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Wilson Waters - Initial contribution
 */
@NonNullByDefault
public class ZabbixBindingConstants {

    private static final String BINDING_ID = "zabbix";

    // Zabbix say "We guarantee feature backward compatibility inside of a major version. When making backward
    // incompatible changes between major releases, we usually leave the old features as deprecated in the next release,
    // and only remove them in the release after that."
    // https://www.zabbix.com/documentation/current/manual/api
    private static final int ZABBIX_API_MAJOR_VERSION = 4;

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SERVER_BRIDGE = new ThingTypeUID(BINDING_ID, "server");
    public static final ThingTypeUID THING_TYPE_HOST = new ThingTypeUID(BINDING_ID, "host");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_SERVER_BRIDGE, THING_TYPE_HOST).collect(Collectors.toSet()));

    // List of properties
    public static final String HOST_PROPERTY_HOSTNAME = "host";

    // List of all Channel ids
    public static final String CHANNEL_1 = "channel1";

    // Other Zabbix specific constants
    public static final String ZABBIX_API_URL = "%SCHEME%://%IP%:%PORT%/%PATH%api_jsonrpc.php";
}
