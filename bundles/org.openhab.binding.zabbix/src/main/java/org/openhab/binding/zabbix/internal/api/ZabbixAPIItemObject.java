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
 * An item returned by the item.get API call.
 *
 * @author Wilson Waters - Initial addition 20200212
 */
public class ZabbixAPIItemObject {

    /** (readonly) ID of the item. **/
    public String itemid;

    /** Update interval of the item. **/
    public String delay;

    /** ID of the host or template that the item belongs to. **/
    public String hostid;

    /** Item key. **/
    public String key_;

    /** Name of the item. **/
    public String name;

    /** Type of the item. **/
    public int type;

    /**
     * Type of information of the item.
     * Possible values:
     * 0 - numeric float;
     * 1 - character;
     * 2 - log;
     * 3 - numeric unsigned;
     * 4 - text.
     **/
    public int value_type;

    /** Description of the item. **/
    public String description;

    /** (readonly) Error text if there are problems updating the item. **/
    public String error;

    /** (readonly) Time when the item was last updated. **/
    public int lastclock;

    /** (readonly) Nanoseconds when the item was last updated. **/
    public int lastns;

    /**
     * (readonly) State of the item.
     *
     * Possible values:
     * 0 - (default) normal;
     * 1 - not supported.
     **/
    public int state;

    /**
     * Status of the item.
     *
     * Possible values:
     * 0 - (default) enabled item;
     * 1 - disabled item.
     **/
    public int status;

    /** Value units (i.e. kW, b/s) **/
    public String units;

}
