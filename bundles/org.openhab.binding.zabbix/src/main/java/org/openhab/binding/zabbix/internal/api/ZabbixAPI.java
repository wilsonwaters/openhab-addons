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

import java.util.Collection;
import java.util.List;

/**
 * Definition of the Zabbix server API in Java. This matches the Zabbix API as closely as possible, hence
 * may be slightly overkill/obscure at times. Keeping the definition as close to the API as possible should
 * help with any future changes.
 * The naming of functions in this class should closely match the Zabbix API names. i.e. history.get is named
 * historyGet().
 *
 * A this stage only the bare minimum of the API is implemented for the purpose of getting it working with OpenHAB.
 * Adding further functionality to this interface (and implemention) should not significantly break anything which
 * uses this interface.
 *
 * Generally you will want to use the helper classes {@link ZabbixAPIHelper} to abstract away details of the API
 * calls. A lot of functions require multiple API calls to complete.
 *
 * @author Wilson Waters - Initial addition 20200212
 */
public interface ZabbixAPI {

    /**
     * The method allows to retrieve hosts according to the given parameters.
     *
     * The initial implementation of this API call simply gets all known hosts without filtering
     *
     * {@link https://www.zabbix.com/documentation/current/manual/api/reference/host/get}
     *
     * @return all known hosts configured in Zabbix
     */
    List<ZabbixAPIHostObject> hostGet() throws ZabbixAPIException, ZabbixCommunicationException;

    /**
     * The method allows to retrieve items according to the given parameters.
     *
     * The initial implementation of this API call allows filtering by hostid only. Set to null if you
     * want to receive all items known to the system (warning, it'll be a huge response!)
     *
     * {@link https://www.zabbix.com/documentation/current/manual/api/reference/item/get}
     *
     * @param hostids
     * @return all items matching the filters
     */
    List<ZabbixAPIItemObject> itemGet(Collection<String> hostids)
            throws ZabbixAPIException, ZabbixCommunicationException;

    /**
     * The method allows to retrieve history data according to the given parameters.
     *
     * Any parameter can be null which will exclude the parameter from the request and server will use defaults.
     *
     * {@link https://www.zabbix.com/documentation/4.0/manual/api/reference/history/get}
     *
     * @param historyType History object types to return. 0 - numeric float; 1 - character; 2 - log; 3 - numeric
     *            unsigned; 4 - text. Default: 3.
     * @param hostIds Return only history from the given hosts.
     * @param itemIds Return only history from the given items.
     * @param timeFrom Return only values that have been received after or at the given time.
     * @param timeTill Return only values that have been received before or at the given time.
     * @param sortField Sort the result by the given properties. Possible values are: itemid and clock.
     * @param limit Limit the number of records returned.
     * @return a list of {@link HistoryItems} containing the data requested
     */
    List<ZabbixAPIHistoryObject<?>> historyGet(Integer historyType, Collection<String> hostIds,
            Collection<String> itemIds, Long timeFrom, Long timeTill, String sortField, Integer limit)
            throws ZabbixAPIException, ZabbixCommunicationException;

}
