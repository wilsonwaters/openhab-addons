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
package org.openhab.binding.lutron.internal.radiora.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.transport.serial.SerialPortManager;
import org.openhab.binding.lutron.internal.radiora.RS232Connection;
import org.openhab.binding.lutron.internal.radiora.RadioRAConnection;
import org.openhab.binding.lutron.internal.radiora.RadioRAConnectionException;
import org.openhab.binding.lutron.internal.radiora.RadioRAFeedbackListener;
import org.openhab.binding.lutron.internal.radiora.config.RS232Config;
import org.openhab.binding.lutron.internal.radiora.protocol.RadioRACommand;
import org.openhab.binding.lutron.internal.radiora.protocol.RadioRAFeedback;
import org.openhab.binding.lutron.internal.radiora.protocol.ZoneMapInquiryCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RS232Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jeff Lauterbach - Initial contribution
 */
public class RS232Handler extends BaseBridgeHandler implements RadioRAFeedbackListener {

    private Logger logger = LoggerFactory.getLogger(RS232Handler.class);

    private RadioRAConnection connection;

    private ScheduledFuture<?> zoneMapScheduledTask;

    public RS232Handler(Bridge bridge, SerialPortManager serialPortManager) {
        super(bridge);

        this.connection = new RS232Connection(serialPortManager);
        this.connection.setListener(this);
    }

    @Override
    public void dispose() {
        if (zoneMapScheduledTask != null) {
            zoneMapScheduledTask.cancel(true);
        }

        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void initialize() {
        connectToRS232();

        scheduleZoneMapQuery();
    }

    protected void connectToRS232() {
        RS232Config config = getConfigAs(RS232Config.class);
        String portName = config.getPortName();
        int baud = config.getBaud();

        logger.debug("Attempting to connect to RS232 on port {}", portName);

        try {
            connection.open(portName, baud);
        } catch (RadioRAConnectionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, e.getMessage());
            return;
        }

        logger.debug("Connected successfully");

        updateStatus(ThingStatus.ONLINE);
    }

    protected void scheduleZoneMapQuery() {
        RS232Config config = getConfigAs(RS232Config.class);
        logger.debug("Scheduling zone map query at {} second inverval", config.getZoneMapQueryInterval());

        Runnable task = () -> sendCommand(new ZoneMapInquiryCommand());

        zoneMapScheduledTask = this.scheduler.scheduleWithFixedDelay(task, 3, config.getZoneMapQueryInterval(),
                TimeUnit.SECONDS);
    }

    public void sendCommand(RadioRACommand command) {
        connection.write(command.toString());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void handleRadioRAFeedback(RadioRAFeedback feedback) {
        for (Thing thing : getThing().getThings()) {
            ThingHandler handler = thing.getHandler();
            if (handler instanceof LutronHandler) {
                ((LutronHandler) handler).handleFeedback(feedback);
            } else {
                logger.debug("Unexpected - Thing {} is not a LutronHandler", thing.getClass());
            }
        }
    }
}
