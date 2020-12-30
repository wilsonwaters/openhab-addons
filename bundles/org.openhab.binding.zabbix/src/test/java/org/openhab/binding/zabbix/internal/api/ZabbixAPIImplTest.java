package org.openhab.binding.zabbix.internal.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test needs a Zabbix server to test with. It's more of an integration test than a unit test.
 * Pass parameters through jvm system properties.
 * -DZABBIX_HOSTNAME=localhost -DZABBIX_USERNAME=openhab -DZABBIX_PASSWORD=openhab
 */
public class ZabbixAPIImplTest {

    private ZabbixAPIImpl api;

    @Before
    public void setUp() throws Exception {
        String hostname = System.getProperty("ZABBIX_HOSTNAME", "localhost");
        String username = System.getProperty("ZABBIX_USERNAME", "openhab");
        String password = System.getProperty("ZABBIX_PASSWORD", "openhab");
        this.api = new ZabbixAPIImpl();
        this.api.setHostname(hostname);
        this.api.setCredentials(username, password);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testHostGet() throws ZabbixCommunicationException, ZabbixAPIException {
        List<ZabbixAPIHostObject> hosts = api.hostGet();
        hosts.forEach(host -> System.out.println(host.host + " (id=" + host.hostid + ")"));
        assertEquals(27, hosts.size());
    }

    @Test
    public void testItemGet() throws ZabbixCommunicationException, ZabbixAPIException {
        ArrayList<String> hostIds = new ArrayList<String>();
        hostIds.add("10109");
        List<ZabbixAPIItemObject> items = api.itemGet(hostIds);
        items.forEach(item -> System.out.println(item.name + " (id=" + item.itemid + ")" + item.key_));
        assertEquals(27, items.size());
    }

    @Test
    public void testHistoryGet() {
        fail("Not yet implemented");
    }

}
