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
package org.openhab.binding.openuv.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link OpenUVBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Gaël L'hopital - Initial contribution
 */
@NonNullByDefault
public class OpenUVBindingConstants {
    public static final String BASE_URL = "https://api.openuv.io/api/v1/uv";
    public static final String BINDING_ID = "openuv";
    public static final String LOCAL = "local";

    public static final String LOCATION = "location";
    public static final String APIKEY = "apikey";

    // List of Bridge Type UIDs
    public static final ThingTypeUID APIBRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, "openuvapi");

    // List of Things Type UIDs
    public static final ThingTypeUID LOCATION_REPORT_THING_TYPE = new ThingTypeUID(BINDING_ID, "uvreport");

    // List of all Channel id's
    public static final String UV_INDEX = "UVIndex";
    public static final String UV_COLOR = "UVColor";
    public static final String UV_MAX = "UVMax";
    public static final String UV_MAX_TIME = "UVMaxTime";
    public static final String UV_MAX_EVENT = "UVMaxEvent";
    public static final String OZONE = "Ozone";
    public static final String OZONE_TIME = "OzoneTime";
    public static final String UV_TIME = "UVTime";
    public static final String SAFE_EXPOSURE = "SafeExposure";
    public static final String ELEVATION = "elevation";

    public static final Set<ThingTypeUID> BRIDGE_THING_TYPES_UIDS = Collections.singleton(APIBRIDGE_THING_TYPE);
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>(
            Arrays.asList(LOCATION_REPORT_THING_TYPE));
}
