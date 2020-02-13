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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Default implementation of the Zabbix API
 *
 * @author Wilson Waters - Initial addition 20200212
 */
public class ZabbixAPIImpl implements ZabbixAPI {

    private final Logger logger = LoggerFactory.getLogger(ZabbixAPIImpl.class);
    private static final String ZABBIX_API_PATH = "/zabbix/api_jsonrpc.php";
    private static final String ZABBIX_API_CONTENTTYPE = "application/json-rpc";
    private static final int TIMEOUT_MS = 30000;

    private String apiUrl;
    private String hostname;

    private Gson gson;

    public ZabbixAPIImpl() {
        this.apiUrl = null;
        GsonBuilder builder = new GsonBuilder();
        this.gson = builder.create();
    }

    /**
     * Hostname or IP address of the zabbix server.
     * apiURL will override this if set.
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
        this.apiUrl = null; // reset the API URL so it gets re-generated
    }

    /**
     * Override the default Zabbix API URL. Hostname will be ignored.
     */
    public void setApiUrl(String apiURL) {
        this.apiUrl = apiURL;
    }

    @Override
    public String userLogin(String username, String password) throws ZabbixAPIException, ZabbixCommunicationException {

        logger.debug("Logging into zabbix server with username: {}", username);
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("username", username);
        requestBody.addProperty("password", password);

        String response;
        synchronized (this) {
            try {
                response = HttpUtil.executeUrl("POST", getApiUrl(),
                        new ByteArrayInputStream(gson.toJson(requestBody).getBytes(Charset.forName("UTF-8"))),
                        ZABBIX_API_CONTENTTYPE, TIMEOUT_MS);
            } catch (IOException ex) {
                throw new ZabbixCommunicationException("Zabbix server API call failed", ex);
            }
        }

        if (response == null) {
            // null response means HttpUtil.execureUrl failed parsing input
            throw new ZabbixCommunicationException("Zabbix server API call failed to execute");
        }

        // parse response
        String token;
        try {
            ZabbixAPILoginObject login = gson.fromJson(response, ZabbixAPILoginObject.class);
            token = login.result;
        } catch (JsonParseException ex) {
            throw new ZabbixAPIException("Error parsing response from zabbix server", ex);
        }

        return token;
    }

    @Override
    public List<ZabbixAPIHostObject> hostGet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ZabbixAPIItemObject> itemGet(Collection<String> hostids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ZabbixAPIHistoryObject<?>> historyGet(Integer historyType, Collection<String> hostIds,
            Collection<String> itemIds, Long timeFrom, Long timeTill, String sortField, Integer limit) {
        // TODO Auto-generated method stub
        return null;
    }

    private String getApiUrl() throws ZabbixCommunicationException {
        if (this.apiUrl == null) {
            if (this.hostname == null || this.hostname.isEmpty()) {
                throw new ZabbixCommunicationException(
                        "hostname has not been configured. Must configure either hostname or apiUrl");
            }
            this.apiUrl = "http://" + this.hostname + ZABBIX_API_PATH;
        }
        return this.apiUrl;
    }

}
