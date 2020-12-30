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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.zabbix.internal.ZabbixBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Default implementation of the Zabbix API.
 *
 * Based off Zabbix API docs https://www.zabbix.com/documentation/4.0/manual/api/reference
 * The API follows JSON-RPC https://www.jsonrpc.org/specification
 *
 * NOTE: This is not feature complete. It simply implements the functions
 * required for the OpenHAB Binding in the most simple way.
 * If this class gets bigger, you should investigate using one one of the
 * existing Zabbix API libraries listed here https://zabbix.org/wiki/Docs/api/libraries
 *
 * @see https://www.zabbix.com/documentation/4.0/manual/api/reference
 *
 * @author Wilson Waters - Initial addition 20200212
 */
public class ZabbixAPIImpl implements ZabbixAPI {

    private final Logger logger = LoggerFactory.getLogger(ZabbixAPIImpl.class);
    private static final String ZABBIX_API_CONTENTTYPE = "application/json-rpc";
    private static final String ZABBIX_API_JSONRPV_VERSION = "2.0";
    private static final int TIMEOUT_MS = 30000;

    private String apiUrlOverride; // Manually configured URL. Don't try to auto-guess API address when this is set,
                                   // just use as-is
    private String apiUrl;
    private String hostname;
    private String scheme = "https";
    private int port = 0;
    private String path = "zabbix/";

    private String username;
    private String password;
    private String apiToken;

    private Gson gson;

    // Default OpenHAB HttpUtils does not expose the response code. So need to use Jetty
    // https://community.openhab.org/t/oh2-binding-accessing-via-http-which-library-should-be-used/33153/9
    private @NonNullByDefault({}) org.eclipse.jetty.client.HttpClient httpClient;

    /**
     * Each "method" handled by the Zabbix API which we implement.
     * The methodName parameter corresponds to the method string used in the Zabbix API call
     */
    private enum Method {
        USER_LOGIN("user.login"),
        HOST_GET("host.get"),
        ITEM_GET("item.get"),
        HISTORY_GET("history.get");

        private String methodName;

        Method(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }
    }

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
        if (apiURL != null && !apiURL.isEmpty()) {
            this.apiUrlOverride = apiURL;
            this.apiUrl = null; // reset the API URL so it gets re-generated
        }
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        this.apiToken = null;
    }

    // @Reference
    protected void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    protected void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = null;
    }

    @Override
    public List<ZabbixAPIHostObject> hostGet() throws ZabbixAPIException, ZabbixCommunicationException {
        String zabbixParams = "{\"sortfield\": \"host\"}";
        JsonObject jsonParams = new JsonParser().parse(zabbixParams).getAsJsonObject();
        JsonElement response = doAPICall(Method.HOST_GET, jsonParams, true);

        ArrayList<ZabbixAPIHostObject> result = new ArrayList<ZabbixAPIHostObject>();
        response.getAsJsonArray().forEach(hostElement -> {
            ZabbixAPIHostObject hostObject = new ZabbixAPIHostObject();
            hostObject.host = hostElement.getAsJsonObject().get("host").getAsString();
            hostObject.hostid = hostElement.getAsJsonObject().get("hostid").getAsString();
            hostObject.available = hostElement.getAsJsonObject().get("available").getAsInt();
            hostObject.description = hostElement.getAsJsonObject().get("description").getAsString();
            hostObject.error = hostElement.getAsJsonObject().get("error").getAsString();
            result.add(hostObject);
        });
        return result;
    }

    @Override
    public List<ZabbixAPIItemObject> itemGet(Collection<String> hostids)
            throws ZabbixAPIException, ZabbixCommunicationException {
        String zabbixParams = "{\"sortfield\": \"name\", \"hostids\": [";
        zabbixParams += hostids.stream().collect(Collectors.joining(", "));
        zabbixParams += "]}";
        JsonObject jsonParams = new JsonParser().parse(zabbixParams).getAsJsonObject();
        JsonElement response = doAPICall(Method.ITEM_GET, jsonParams, true);

        ArrayList<ZabbixAPIItemObject> result = new ArrayList<ZabbixAPIItemObject>();
        response.getAsJsonArray().forEach(itemElement -> {
            ZabbixAPIItemObject itemObject = new ZabbixAPIItemObject();
            itemObject.delay = itemElement.getAsJsonObject().get("delay").getAsString();
            itemObject.description = itemElement.getAsJsonObject().get("description").getAsString();
            itemObject.error = itemElement.getAsJsonObject().get("error").getAsString();
            itemObject.hostid = itemElement.getAsJsonObject().get("hostid").getAsString();
            itemObject.itemid = itemElement.getAsJsonObject().get("itemid").getAsString();
            itemObject.key_ = itemElement.getAsJsonObject().get("key_").getAsString();
            itemObject.lastclock = itemElement.getAsJsonObject().get("lastclock").getAsInt();
            itemObject.lastns = itemElement.getAsJsonObject().get("lastns").getAsInt();
            itemObject.name = itemElement.getAsJsonObject().get("name").getAsString();
            itemObject.state = itemElement.getAsJsonObject().get("state").getAsInt();
            itemObject.status = itemElement.getAsJsonObject().get("status").getAsInt();
            itemObject.type = itemElement.getAsJsonObject().get("type").getAsInt();
            itemObject.units = itemElement.getAsJsonObject().get("units").getAsString();
            itemObject.value_type = itemElement.getAsJsonObject().get("value_type").getAsInt();
            result.add(itemObject);
        });
        return result;
    }

    @Override
    public List<ZabbixAPIHistoryObject<?>> historyGet(Integer historyType, Collection<String> hostIds,
            Collection<String> itemIds, Long timeFrom, Long timeTill, String sortField, Integer limit) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Perform the API call to Zabbix.
     *
     * Handles common API functionality, including authentication.
     * If a token hasn't been set or the call returns with 401 "unauthenticated", then the
     * user.login method is called to generate a new API token. The original API call is then
     * retried with authentication.
     *
     * Use of this function assumes knowledge of the parameter and response JSON structure. The parameter
     * string is simply passed through without validation. Likewise, the response is passed back without
     * inspection.
     *
     * Note that the default OpenHAB HttpUtils does not expose the response code. So need to use Jetty
     * https://community.openhab.org/t/oh2-binding-accessing-via-http-which-library-should-be-used/33153/9
     *
     * @param method the Zabbix API call to make
     * @param paramsJson the Zabbix API "params" field, in json format. or Null for no parameters
     * @param authenticateIfRequired normally true. If false and session is not valid, an ZabbixCommunicationException
     *            will be thrown
     * @return the result field from the JSON response.
     */
    private JsonElement doAPICall(Method method, JsonObject paramsJson, boolean authenticateIfRequired)
            throws ZabbixCommunicationException, ZabbixAPIException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("jsonrpc", ZABBIX_API_JSONRPV_VERSION);
        requestBody.addProperty("method", method.getMethodName());
        if (paramsJson != null) {
            requestBody.add("params", paramsJson);
        }
        requestBody.addProperty("auth", this.apiToken);
        requestBody.addProperty("id", "1"); // Not sure why we would use anything other than a constant value here?

        ContentResponse response;
        HttpClient httpClient = null;
        try {
            httpClient = getHttpClient();
        } catch (Exception e) {
            throw new ZabbixCommunicationException(
                    "Zabbix server API call (" + method.getMethodName() + ") failed (creating http client)", e);
        }
        synchronized (this) {
            try {
                final Request request = httpClient.newRequest(getApiUrl()).method(HttpMethod.POST).timeout(TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
                try (final InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(
                        new ByteArrayInputStream(requestBody.toString().getBytes("UTF8")))) {
                    request.content(inputStreamContentProvider, ZABBIX_API_CONTENTTYPE);
                }

                response = request.send();

                if (response.getStatus() != 200) {
                    throw new ZabbixCommunicationException("Zabbix server API call (" + method.getMethodName()
                            + ") failed. Response status code " + response.getStatus());
                }

            } catch (IOException e) {
                throw new ZabbixCommunicationException(
                        "Zabbix server API call (" + method.getMethodName() + ") failed. " + e.getMessage(), e);
            } catch (InterruptedException e) {
                throw new ZabbixCommunicationException(
                        "Zabbix server API call (" + method.getMethodName() + ") failed (Inturrupted)", e);
            } catch (TimeoutException e) {
                throw new ZabbixCommunicationException(
                        "Zabbix server API call (" + method.getMethodName() + ") failed (Timeout)", e);
            } catch (ExecutionException e) {
                // special case, try again one more time with http rather than https
                if (this.scheme == "https") {
                    this.scheme = "http";
                    return doAPICall(method, paramsJson, authenticateIfRequired);
                }
                throw new ZabbixCommunicationException("Zabbix server API call (" + method.getMethodName()
                        + ") failed (Execution failed to API endpoint " + getApiUrl() + ")", e);
            }
        }

        // parse response
        JsonParser parser = new JsonParser();
        JsonElement responseRoot = null;
        JsonElement resultJsonElement = null;
        try {
            responseRoot = parser.parse(response.getContentAsString());
            resultJsonElement = responseRoot.getAsJsonObject().get("result");
        } catch (JsonParseException e) {
            throw new ZabbixAPIException("Zabbix server API call (" + method.getMethodName()
                    + ") failed (could not parse API result. Is the API endpoint correct?)", e);
        }

        if (resultJsonElement == null) {
            try {
                JsonObject resultJsonError = responseRoot.getAsJsonObject().get("error").getAsJsonObject();
                String message = resultJsonError.get("message").getAsString();
                String data = resultJsonError.get("data").getAsString();

                // handle non-authenticated request.
                // Ideally, zabbix would return a 401 status. Instead it returns 200 with a specific error message.
                // So we have to handle this all the way down here instead of relying on a 401 status.
                if (authenticateIfRequired && message.equals("Invalid params.") && data.equals("Not authorised.")) {
                    // need to authenticate and try again. Either we haven't logged in yet, or the token expired
                    this.apiToken = null;
                    userLogin();
                    return doAPICall(method, paramsJson, false); // call ourselves again. Make sure we don't get stuck
                                                                 // in a
                    // recursion loop if auth is failing weirdly
                }

                throw new ZabbixAPIException("Zabbix server API call (" + method.getMethodName()
                        + ") failed (could not parse response): " + message + " " + data);
            } finally {
            }
        }

        return resultJsonElement;
    }

    /**
     * I can't work out why HttpClient doesn't get injected!! aargh.
     * Workaround - just create a new client if one hasn't already been set.
     *
     * @return
     * @throws Exception
     */
    private HttpClient getHttpClient() throws Exception {
        if (this.httpClient == null) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            this.httpClient = new HttpClient(sslContextFactory);
            this.httpClient.start();
        }
        return this.httpClient;
    }

    /**
     * Logs the specified user into zabbix and returns a API token which must be used for future communications.
     *
     * The user must exist on the Zabbix server and must have access to any hosts required.
     *
     * API token is saved to internal state and used for future api calls
     *
     * {@link https://www.zabbix.com/documentation/current/manual/api/reference/user/login}
     *
     * @param username
     * @param password
     */
    private void userLogin() throws ZabbixAPIException, ZabbixCommunicationException {

        logger.debug("Logging into zabbix server with username: {}", username);
        JsonObject jsonParams = new JsonObject();
        jsonParams.addProperty("user", this.username);
        jsonParams.addProperty("password", this.password);
        JsonElement response = doAPICall(Method.USER_LOGIN, jsonParams, false);

        if (response.isJsonPrimitive()) {
            this.apiToken = response.getAsString();
        } else {
            throw new ZabbixAPIException("Zabbix server API login failed (Could not get token)");
        }
    }

    private String getApiUrl() throws ZabbixCommunicationException {
        if (this.apiUrl == null) {
            if (this.apiUrlOverride != null && !this.apiUrlOverride.isEmpty()) {
                this.apiUrl = this.apiUrlOverride;
            } else if (this.hostname != null && !this.hostname.isEmpty()) {
                int connectPort = this.port;
                if (connectPort == 0 && this.scheme == "https") {
                    connectPort = 443;
                } else if (connectPort == 0 && this.scheme == "http") {
                    connectPort = 80;
                }
                this.apiUrl = ZabbixBindingConstants.ZABBIX_API_URL.replaceFirst("%SCHEME%", this.scheme)
                        .replaceFirst("%IP%", this.hostname).replaceFirst("%PORT%", Integer.toString(connectPort))
                        .replaceFirst("%PATH%", this.path);
            } else {
                throw new ZabbixCommunicationException(
                        "hostname has not been configured. Must configure either hostname or apiUrl");
            }
        }
        return this.apiUrl;
    }

}
