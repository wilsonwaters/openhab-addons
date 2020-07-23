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

/**
 * The {@link ZabbixServerConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Wilson Waters
 *         Initial addition 20200212
 */
public class ZabbixServerConfiguration {

    public String hostname;
    public Integer refreshInterval;
    public String apiUrl;
}
