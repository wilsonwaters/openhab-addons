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
package org.openhab.binding.zabbix.internal.api;

/**
 * An item returned by the host.get API call.
 *
 * @author Wilson Waters - Initial addition 20200212
 */
public class ZabbixAPIHostObject {

    /** (readonly) ID of the host. **/
    public String hostid;

    /** Technical name of the host. **/
    public String host;

    /**
     * (readonly) Availability of Zabbix agent.
     *
     * Possible values are:
     * 0 - (default) unknown;
     * 1 - available;
     * 2 - unavailable.
     **/
    public int available;

    /** Description of the host. **/
    public String description;

    /** (readonly) Error text if Zabbix agent is unavailable. **/
    public String error;

}
