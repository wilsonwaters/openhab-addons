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
package org.openhab.binding.openthermgateway;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link OpenThermGatewayBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Arjen Korevaar - Initial contribution
 */
@NonNullByDefault
public class OpenThermGatewayBindingConstants {

    private static final String BINDING_ID = "openthermgateway";

    // List of all Thing Type UID's
    public static final ThingTypeUID MAIN_THING_TYPE = new ThingTypeUID(BINDING_ID, "otgw");

    // List of all Channel id's
    public static final String CHANNEL_SEND_COMMAND = "sendcommand";

    public static final String CHANNEL_OVERRIDE_SETPOINT_TEMPORARY = "temperaturetemporary";
    public static final String CHANNEL_OVERRIDE_SETPOINT_CONSTANT = "temperatureconstant";
    public static final String CHANNEL_OVERRIDE_DHW_SETPOINT = "overridedhwsetpoint";
    public static final String CHANNEL_ROOM_TEMPERATURE = "roomtemp";
    public static final String CHANNEL_ROOM_SETPOINT = "roomsetpoint";
    public static final String CHANNEL_FLOW_TEMPERATURE = "flowtemp";
    public static final String CHANNEL_RETURN_TEMPERATURE = "returntemp";
    public static final String CHANNEL_OUTSIDE_TEMPERATURE = "outsidetemp";
    public static final String CHANNEL_CENTRAL_HEATING_WATER_SETPOINT = "controlsetpoint";
    public static final String CHANNEL_CENTRAL_HEATING_WATER_PRESSURE = "waterpressure";
    public static final String CHANNEL_CENTRAL_HEATING_ENABLED = "ch_enable";
    public static final String CHANNEL_CENTRAL_HEATING_MODE = "ch_mode";
    public static final String CHANNEL_DOMESTIC_HOT_WATER_TEMPERATURE = "dhwtemp";
    public static final String CHANNEL_DOMESTIC_HOT_WATER_ENABLED = "dhw_enable";
    public static final String CHANNEL_DOMESTIC_HOT_WATER_MODE = "dhw_mode";
    public static final String CHANNEL_DOMESTIC_HOT_WATER_SETPOINT = "tdhwset";
    public static final String CHANNEL_FLAME = "flame";
    public static final String CHANNEL_RELATIVE_MODULATION_LEVEL = "modulevel";
    public static final String CHANNEL_MAXIMUM_MODULATION_LEVEL = "maxrelmdulevel";
    public static final String CHANNEL_FAULT = "fault";
    public static final String CHANNEL_SERVICEREQUEST = "servicerequest";
    public static final String CHANNEL_REMOTE_RESET = "lockout-reset";
    public static final String CHANNEL_LOW_WATER_PRESSURE = "lowwaterpress";
    public static final String CHANNEL_GAS_FLAME_FAULT = "gasflamefault";
    public static final String CHANNEL_AIR_PRESSURE_FAULT = "airpressfault";
    public static final String CHANNEL_WATER_OVER_TEMP = "waterovtemp";
    public static final String CHANNEL_OEM_FAULTCODE = "oemfaultcode";

    public static final Set<String> SUPPORTED_CHANNEL_IDS = Collections
            .unmodifiableSet(Stream.of(CHANNEL_ROOM_TEMPERATURE, CHANNEL_ROOM_SETPOINT, CHANNEL_FLOW_TEMPERATURE,
                    CHANNEL_RETURN_TEMPERATURE, CHANNEL_OUTSIDE_TEMPERATURE, CHANNEL_CENTRAL_HEATING_WATER_PRESSURE,
                    CHANNEL_CENTRAL_HEATING_ENABLED, CHANNEL_CENTRAL_HEATING_MODE,
                    CHANNEL_CENTRAL_HEATING_WATER_SETPOINT, CHANNEL_DOMESTIC_HOT_WATER_TEMPERATURE,
                    CHANNEL_DOMESTIC_HOT_WATER_ENABLED, CHANNEL_DOMESTIC_HOT_WATER_MODE,
                    CHANNEL_DOMESTIC_HOT_WATER_SETPOINT, CHANNEL_FLAME, CHANNEL_RELATIVE_MODULATION_LEVEL,
                    CHANNEL_MAXIMUM_MODULATION_LEVEL, CHANNEL_FAULT, CHANNEL_SERVICEREQUEST, CHANNEL_REMOTE_RESET,
                    CHANNEL_LOW_WATER_PRESSURE, CHANNEL_GAS_FLAME_FAULT, CHANNEL_AIR_PRESSURE_FAULT,
                    CHANNEL_WATER_OVER_TEMP, CHANNEL_OEM_FAULTCODE).collect(Collectors.toSet()));
}
