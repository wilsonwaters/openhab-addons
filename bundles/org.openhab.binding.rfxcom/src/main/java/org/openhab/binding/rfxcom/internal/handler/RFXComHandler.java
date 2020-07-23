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
package org.openhab.binding.rfxcom.internal.handler;

import static org.openhab.binding.rfxcom.internal.RFXComBindingConstants.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.rfxcom.internal.DeviceMessageListener;
import org.openhab.binding.rfxcom.internal.config.RFXComDeviceConfiguration;
import org.openhab.binding.rfxcom.internal.exceptions.RFXComException;
import org.openhab.binding.rfxcom.internal.exceptions.RFXComMessageNotImplementedException;
import org.openhab.binding.rfxcom.internal.messages.RFXComBaseMessage.PacketType;
import org.openhab.binding.rfxcom.internal.messages.RFXComDeviceMessage;
import org.openhab.binding.rfxcom.internal.messages.RFXComMessage;
import org.openhab.binding.rfxcom.internal.messages.RFXComMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RFXComHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Pauli Anttila - Initial contribution
 */
public class RFXComHandler extends BaseThingHandler implements DeviceMessageListener, DeviceState {
    private static final int LOW_BATTERY_LEVEL = 1;

    private final Logger logger = LoggerFactory.getLogger(RFXComHandler.class);

    private final Map<String, Type> stateMap = new ConcurrentHashMap<>();

    private RFXComBridgeHandler bridgeHandler;
    private RFXComDeviceConfiguration config;

    public RFXComHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);

        if (bridgeHandler != null) {
            if (command instanceof RefreshType) {
                logger.trace("Received unsupported Refresh command");
            } else {
                try {
                    PacketType packetType = RFXComMessageFactory
                            .convertPacketType(getThing().getThingTypeUID().getId().toUpperCase());

                    RFXComMessage msg = RFXComMessageFactory.createMessage(packetType);

                    msg.setConfig(config);
                    msg.convertFromState(channelUID.getId(), command);

                    bridgeHandler.sendMessage(msg);
                } catch (RFXComMessageNotImplementedException e) {
                    logger.error("Message not supported", e);
                } catch (RFXComException e) {
                    logger.error("Transmitting error", e);
                }
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing thing {}", getThing().getUID());
        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(),
                (getBridge() == null) ? null : getBridge().getStatus());

        stateMap.clear();
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {} for thing {}", bridgeStatusInfo, getThing().getUID());
        initializeBridge((getBridge() == null) ? null : getBridge().getHandler(), bridgeStatusInfo.getStatus());
    }

    private void initializeBridge(ThingHandler thingHandler, ThingStatus bridgeStatus) {
        logger.debug("initializeBridge {} for thing {}", bridgeStatus, getThing().getUID());

        config = getConfigAs(RFXComDeviceConfiguration.class);
        if (config.deviceId == null || config.subType == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "RFXCOM device missing deviceId or subType");
        } else if (thingHandler != null && bridgeStatus != null) {
            bridgeHandler = (RFXComBridgeHandler) thingHandler;
            bridgeHandler.registerDeviceStatusListener(this);

            if (bridgeStatus == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
        }
    }

    @Override
    public void dispose() {
        logger.debug("Thing {} disposed.", getThing().getUID());
        if (bridgeHandler != null) {
            bridgeHandler.unregisterDeviceStatusListener(this);
        }
        bridgeHandler = null;
        super.dispose();
    }

    @Override
    public void onDeviceMessageReceived(ThingUID bridge, RFXComDeviceMessage message) {
        try {
            String id = message.getDeviceId();
            if (config.deviceId.equals(id)) {
                String receivedId = PACKET_TYPE_THING_TYPE_UID_MAP.get(message.getPacketType()).getId();
                logger.debug("Received message from bridge: {} message: {}", bridge, message);

                if (receivedId.equals(getThing().getThingTypeUID().getId())) {
                    updateStatus(ThingStatus.ONLINE);

                    for (Channel channel : getThing().getChannels()) {
                        ChannelUID uid = channel.getUID();
                        String channelId = uid.getId();

                        try {
                            switch (channelId) {
                                case CHANNEL_COMMAND:
                                case CHANNEL_CHIME_SOUND:
                                case CHANNEL_MOOD:
                                    postNullableCommand(uid, message.convertToCommand(channelId, this));
                                    break;

                                case CHANNEL_LOW_BATTERY:
                                    updateNullableState(uid,
                                            isLowBattery(message.convertToState(CHANNEL_BATTERY_LEVEL, this)));
                                    break;

                                default:
                                    updateNullableState(uid, message.convertToState(channelId, this));
                                    break;
                            }
                        } catch (RFXComException e) {
                            logger.trace("{} does not handle {}", channelId, message);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred during message receiving", e);
        }
    }

    private void updateNullableState(ChannelUID uid, State state) {
        if (state == null) {
            return;
        }

        stateMap.put(uid.getId(), state);
        updateState(uid, state);
    }

    private void postNullableCommand(ChannelUID uid, Command command) {
        if (command == null) {
            return;
        }

        stateMap.put(uid.getId(), command);
        postCommand(uid, command);
    }

    @Override
    public Type getLastState(String channelId) {
        return stateMap.get(channelId);
    }

    /**
     * Check if battery level is below low battery threshold level.
     *
     * @param batteryLevel Internal battery level
     * @return OnOffType
     */
    private State isLowBattery(State batteryLevel) {
        int level = ((DecimalType) batteryLevel).intValue();
        if (level <= LOW_BATTERY_LEVEL) {
            return OnOffType.ON;
        } else {
            return OnOffType.OFF;
        }
    }
}
