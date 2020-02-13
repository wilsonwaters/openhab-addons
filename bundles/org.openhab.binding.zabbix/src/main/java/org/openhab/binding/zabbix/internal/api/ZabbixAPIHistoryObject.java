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
 * An item returned by the history.get API call.
 * The type of this class defines what type of data it holds. Currently supports Zabbix Float, Integer and String.
 * Future versions may extend this class to handle Text and Log history if needed.
 *
 * @author Wilson Waters - Initial addition 20200212
 */
public class ZabbixAPIHistoryObject<T> {

    /** ID of the related item **/
    public int itemid;

    /** Time when that value was received **/
    public int clock;

    /** Nanoseconds when the value was received **/
    public int ns;

    /** Received value **/
    public T value;
}
