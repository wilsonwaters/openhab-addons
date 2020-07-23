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
package org.openhab.binding.modbus.internal.handler;

import static org.openhab.binding.modbus.internal.ModbusBindingConstantsInternal.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.modbus.handler.EndpointNotInitializedException;
import org.openhab.binding.modbus.handler.ModbusEndpointThingHandler;
import org.openhab.binding.modbus.handler.ModbusPollerThingHandler;
import org.openhab.binding.modbus.internal.ModbusBindingConstantsInternal;
import org.openhab.binding.modbus.internal.ModbusConfigurationException;
import org.openhab.binding.modbus.internal.Transformation;
import org.openhab.binding.modbus.internal.config.ModbusDataConfiguration;
import org.openhab.io.transport.modbus.AsyncModbusFailure;
import org.openhab.io.transport.modbus.AsyncModbusReadResult;
import org.openhab.io.transport.modbus.AsyncModbusWriteResult;
import org.openhab.io.transport.modbus.BitArray;
import org.openhab.io.transport.modbus.ModbusBitUtilities;
import org.openhab.io.transport.modbus.ModbusCommunicationInterface;
import org.openhab.io.transport.modbus.ModbusConstants;
import org.openhab.io.transport.modbus.ModbusConstants.ValueType;
import org.openhab.io.transport.modbus.ModbusReadFunctionCode;
import org.openhab.io.transport.modbus.ModbusReadRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusRegisterArray;
import org.openhab.io.transport.modbus.ModbusWriteCoilRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteRegisterRequestBlueprint;
import org.openhab.io.transport.modbus.ModbusWriteRequestBlueprint;
import org.openhab.io.transport.modbus.exception.ModbusConnectionException;
import org.openhab.io.transport.modbus.exception.ModbusTransportException;
import org.openhab.io.transport.modbus.json.WriteRequestJsonUtilities;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ModbusDataThingHandler} is responsible for interpreting polled modbus data, as well as handling openHAB
 * commands
 *
 * Thing can be re-initialized by the bridge in case of configuration changes (bridgeStatusChanged).
 * Because of this, initialize, dispose and all callback methods (onRegisters, onBits, onError, onWriteResponse) are
 * synchronized
 * to avoid data race conditions.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class ModbusDataThingHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ModbusDataThingHandler.class);

    private final BundleContext bundleContext;

    private static final Duration MIN_STATUS_INFO_UPDATE_INTERVAL = Duration.ofSeconds(1);
    private static final Map<String, List<Class<? extends State>>> CHANNEL_ID_TO_ACCEPTED_TYPES = new HashMap<>();

    static {
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_SWITCH,
                new SwitchItem("").getAcceptedDataTypes());
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_CONTACT,
                new ContactItem("").getAcceptedDataTypes());
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_DATETIME,
                new DateTimeItem("").getAcceptedDataTypes());
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_DIMMER,
                new DimmerItem("").getAcceptedDataTypes());
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_NUMBER,
                new NumberItem("").getAcceptedDataTypes());
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_STRING,
                new StringItem("").getAcceptedDataTypes());
        CHANNEL_ID_TO_ACCEPTED_TYPES.put(ModbusBindingConstantsInternal.CHANNEL_ROLLERSHUTTER,
                new RollershutterItem("").getAcceptedDataTypes());
    }
    // data channels + 4 for read/write last error/success
    private static final int NUMER_OF_CHANNELS_HINT = CHANNEL_ID_TO_ACCEPTED_TYPES.size() + 4;

    //
    // If you change the below default/initial values, please update the corresponding values in dispose()
    //
    private volatile @Nullable ModbusDataConfiguration config;
    private volatile @Nullable ValueType readValueType;
    private volatile @Nullable ValueType writeValueType;
    private volatile @Nullable Transformation readTransformation;
    private volatile @Nullable Transformation writeTransformation;
    private volatile Optional<Integer> readIndex = Optional.empty();
    private volatile Optional<Integer> readSubIndex = Optional.empty();
    private volatile @Nullable Integer writeStart;
    private volatile int pollStart;
    private volatile int slaveId;
    private volatile @Nullable ModbusReadFunctionCode functionCode;
    private volatile @Nullable ModbusReadRequestBlueprint readRequest;
    private volatile long updateUnchangedValuesEveryMillis;
    private volatile @NonNullByDefault({}) ModbusCommunicationInterface comms;
    private volatile boolean isWriteEnabled;
    private volatile boolean isReadEnabled;
    private volatile boolean writeParametersHavingTransformationOnly;
    private volatile boolean childOfEndpoint;
    private volatile @Nullable ModbusPollerThingHandler pollerHandler;
    private volatile Map<String, ChannelUID> channelCache = new HashMap<>();
    private volatile Map<ChannelUID, Long> channelLastUpdated = new HashMap<>(NUMER_OF_CHANNELS_HINT);
    private volatile Map<ChannelUID, State> channelLastState = new HashMap<>(NUMER_OF_CHANNELS_HINT);

    private volatile LocalDateTime lastStatusInfoUpdate = LocalDateTime.MIN;
    private volatile ThingStatusInfo statusInfo = new ThingStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.NONE,
            null);

    public ModbusDataThingHandler(Thing thing) {
        super(thing);
        this.bundleContext = FrameworkUtil.getBundle(ModbusDataThingHandler.class).getBundleContext();
    }

    @Override
    public synchronized void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("Thing {} '{}' received command '{}' to channel '{}'", getThing().getUID(), getThing().getLabel(),
                command, channelUID);
        ModbusDataConfiguration config = this.config;
        if (config == null) {
            return;
        }

        if (RefreshType.REFRESH == command) {
            ModbusPollerThingHandler poller = pollerHandler;
            if (poller == null) {
                // Data thing must be child of endpoint, and thus write-only.
                // There is no data to update
                return;
            }
            // We *schedule* the REFRESH to avoid dead-lock situation where poller is trying update this
            // data thing with cached data (resulting in deadlock in two synchronized methods: this (handleCommand) and
            // onRegisters.
            scheduler.schedule(() -> poller.refresh(), 0, TimeUnit.SECONDS);
            return;
        } else if (hasConfigurationError()) {
            logger.debug(
                    "Thing {} '{}' command '{}' to channel '{}': Thing has configuration error so ignoring the command",
                    getThing().getUID(), getThing().getLabel(), command, channelUID);
            return;
        } else if (!isWriteEnabled) {
            logger.debug(
                    "Thing {} '{}' command '{}' to channel '{}': no writing configured -> aborting processing command",
                    getThing().getUID(), getThing().getLabel(), command, channelUID);
            return;
        }

        Optional<Command> transformedCommand = transformCommandAndProcessJSON(channelUID, command);
        if (transformedCommand == null) {
            // We have, JSON as transform output (which has been processed) or some error. See
            // transformCommandAndProcessJSON javadoc
            return;
        }

        // We did not have JSON output from the transformation, so writeStart is absolute required. Abort if it is
        // missing
        Integer writeStart = this.writeStart;
        if (writeStart == null) {
            logger.warn(
                    "Thing {} '{}': not processing command {} since writeStart is missing and transformation output is not a JSON",
                    getThing().getUID(), getThing().getLabel(), command);
            return;
        }

        if (!transformedCommand.isPresent()) {
            // transformation failed, return
            logger.warn("Cannot process command {} (of type {}) with channel {} since transformation was unsuccessful",
                    command, command.getClass().getSimpleName(), channelUID);
            return;
        }

        ModbusWriteRequestBlueprint request = requestFromCommand(channelUID, command, config, transformedCommand.get(),
                writeStart);
        if (request == null) {
            return;
        }

        logger.trace("Submitting write task {} to endpoint {}", request, comms.getEndpoint());
        comms.submitOneTimeWrite(request, this::onWriteResponse, this::handleWriteError);
    }

    /**
     * Transform received command using the transformation.
     *
     * In case of JSON as transformation output, the output processed using {@link processJsonTransform}.
     *
     * @param channelUID channel UID corresponding to received command
     * @param command command to be transformed
     * @return transformed command. Null is returned with JSON transformation outputs and configuration errors
     *
     * @see processJsonTransform
     */
    private @Nullable Optional<Command> transformCommandAndProcessJSON(ChannelUID channelUID, Command command) {
        String transformOutput;
        Optional<Command> transformedCommand;
        Transformation writeTransformation = this.writeTransformation;
        if (writeTransformation == null || writeTransformation.isIdentityTransform()) {
            transformedCommand = Optional.of(command);
        } else {
            transformOutput = writeTransformation.transform(bundleContext, command.toString());
            if (transformOutput.contains("[")) {
                processJsonTransform(command, transformOutput);
                return null;
            } else if (writeParametersHavingTransformationOnly) {
                logger.error(
                        "Thing {} seems to have writeTransformation but no other write parameters. Since the transformation did not return a JSON for command '{}' (channel {}), this is a configuration error.",
                        getThing().getUID(), command, channelUID);
                updateStatusIfChanged(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, String.format(
                        "Seems to have writeTransformation but no other write parameters. Since the transformation did not return a JSON for command '%s' (channel %s), this is a configuration error",
                        command, channelUID));
                return null;
            } else {
                transformedCommand = Transformation.tryConvertToCommand(transformOutput);
                logger.trace("Converted transform output '{}' to command '{}' (type {})", transformOutput,
                        transformedCommand.map(c -> c.toString()).orElse("<conversion failed>"),
                        transformedCommand.map(c -> c.getClass().getName()).orElse("<conversion failed>"));
            }
        }
        return transformedCommand;
    }

    private @Nullable ModbusWriteRequestBlueprint requestFromCommand(ChannelUID channelUID, Command origCommand,
            ModbusDataConfiguration config, Command transformedCommand, Integer writeStart) {
        ModbusWriteRequestBlueprint request;
        boolean writeMultiple = config.isWriteMultipleEvenWithSingleRegisterOrCoil();
        String writeType = config.getWriteType();
        if (writeType == null) {
            return null;
        }
        if (writeType.equals(WRITE_TYPE_COIL)) {
            Optional<Boolean> commandAsBoolean = ModbusBitUtilities.translateCommand2Boolean(transformedCommand);
            if (!commandAsBoolean.isPresent()) {
                logger.warn(
                        "Cannot process command {} with channel {} since command is not OnOffType, OpenClosedType or Decimal trying to write to coil. Do not know how to convert to 0/1. Transformed command was '{}'",
                        origCommand, channelUID, transformedCommand);
                return null;
            }
            boolean data = commandAsBoolean.get();
            request = new ModbusWriteCoilRequestBlueprint(slaveId, writeStart, data, writeMultiple,
                    config.getWriteMaxTries());
        } else if (writeType.equals(WRITE_TYPE_HOLDING)) {
            ValueType writeValueType = this.writeValueType;
            if (writeValueType == null) {
                // Should not happen in practice, since we are not in configuration error (checked above)
                // This will make compiler happy anyways with the null checks
                logger.warn("Received command but write value type not set! Ignoring command");
                return null;
            }
            ModbusRegisterArray data = ModbusBitUtilities.commandToRegisters(transformedCommand, writeValueType);
            writeMultiple = writeMultiple || data.size() > 1;
            request = new ModbusWriteRegisterRequestBlueprint(slaveId, writeStart, data, writeMultiple,
                    config.getWriteMaxTries());
        } else {
            // Should not happen! This method is not called in case configuration errors and writeType is validated
            // already in initialization (validateAndParseWriteParameters).
            // We keep this here for future-proofing the code (new writeType values)
            throw new NotImplementedException(String.format(
                    "writeType does not equal %s or %s and thus configuration is invalid. Should not end up this far with configuration error.",
                    WRITE_TYPE_COIL, WRITE_TYPE_HOLDING));
        }
        return request;
    }

    private void processJsonTransform(Command command, String transformOutput) {
        ModbusCommunicationInterface localComms = this.comms;
        if (localComms == null) {
            return;
        }
        Collection<ModbusWriteRequestBlueprint> requests;
        try {
            requests = WriteRequestJsonUtilities.fromJson(slaveId, transformOutput);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn(
                    "Thing {} '{}' could handle transformation result '{}'. Original command {}. Error details follow",
                    getThing().getUID(), getThing().getLabel(), transformOutput, command, e);
            return;
        }

        requests.stream().forEach(request -> {
            logger.trace("Submitting write request: {} to endpoint {} (based from transformation {})", request,
                    localComms.getEndpoint(), transformOutput);
            localComms.submitOneTimeWrite(request, this::onWriteResponse, this::handleWriteError);
        });
    }

    @Override
    public synchronized void initialize() {
        // Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        try {
            logger.trace("initialize() of thing {} '{}' starting", thing.getUID(), thing.getLabel());
            ModbusDataConfiguration localConfig = config = getConfigAs(ModbusDataConfiguration.class);
            updateUnchangedValuesEveryMillis = localConfig.getUpdateUnchangedValuesEveryMillis();
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.debug("Thing {} '{}' has no bridge", getThing().getUID(), getThing().getLabel());
                updateStatusIfChanged(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "No poller bridge");
                return;
            }
            BridgeHandler bridgeHandler = bridge.getHandler();
            if (bridgeHandler == null) {
                logger.warn("Bridge {} '{}' has no handler.", bridge.getUID(), bridge.getLabel());
                String errmsg = String.format("Bridge %s '%s' configuration incomplete or with errors", bridge.getUID(),
                        bridge.getLabel());
                throw new ModbusConfigurationException(errmsg);
            }
            if (bridgeHandler instanceof ModbusEndpointThingHandler) {
                // Write-only thing, parent is endpoint
                ModbusEndpointThingHandler endpointHandler = (ModbusEndpointThingHandler) bridgeHandler;
                slaveId = endpointHandler.getSlaveId();
                comms = endpointHandler.getCommunicationInterface();
                childOfEndpoint = true;
                functionCode = null;
                readRequest = null;
            } else {
                ModbusPollerThingHandler localPollerHandler = (ModbusPollerThingHandler) bridgeHandler;
                pollerHandler = localPollerHandler;
                ModbusReadRequestBlueprint localReadRequest = localPollerHandler.getRequest();
                if (localReadRequest == null) {
                    logger.debug("Poller {} '{}' has no read request -- configuration is changing?", bridge.getUID(),
                            bridge.getLabel());
                    updateStatusIfChanged(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                            String.format("Poller %s '%s' has no poll task", bridge.getUID(), bridge.getLabel()));
                    return;
                }
                readRequest = localReadRequest;
                slaveId = localReadRequest.getUnitID();
                functionCode = localReadRequest.getFunctionCode();
                comms = localPollerHandler.getCommunicationInterface();
                pollStart = localReadRequest.getReference();
                childOfEndpoint = false;
            }
            validateAndParseReadParameters(localConfig);
            validateAndParseWriteParameters(localConfig);
            validateMustReadOrWrite();

            updateStatusIfChanged(ThingStatus.ONLINE);
        } catch (ModbusConfigurationException | EndpointNotInitializedException e) {
            logger.debug("Thing {} '{}' initialization error: {}", getThing().getUID(), getThing().getLabel(),
                    e.getMessage());
            updateStatusIfChanged(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        } finally {
            logger.trace("initialize() of thing {} '{}' finished", thing.getUID(), thing.getLabel());
        }
    }

    @Override
    public synchronized void dispose() {
        config = null;
        readValueType = null;
        writeValueType = null;
        readTransformation = null;
        writeTransformation = null;
        readIndex = Optional.empty();
        readSubIndex = Optional.empty();
        writeStart = null;
        pollStart = 0;
        slaveId = 0;
        comms = null;
        functionCode = null;
        readRequest = null;
        isWriteEnabled = false;
        isReadEnabled = false;
        writeParametersHavingTransformationOnly = false;
        childOfEndpoint = false;
        pollerHandler = null;
        channelCache = new HashMap<>();
        lastStatusInfoUpdate = LocalDateTime.MIN;
        statusInfo = new ThingStatusInfo(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, null);
        channelLastUpdated = new HashMap<>(NUMER_OF_CHANNELS_HINT);
        channelLastState = new HashMap<>(NUMER_OF_CHANNELS_HINT);
    }

    @Override
    public synchronized void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged for {}. Reseting handler", this.getThing().getUID());
        this.dispose();
        this.initialize();
    }

    private boolean hasConfigurationError() {
        ThingStatusInfo statusInfo = getThing().getStatusInfo();
        return statusInfo.getStatus() == ThingStatus.OFFLINE
                && statusInfo.getStatusDetail() == ThingStatusDetail.CONFIGURATION_ERROR;
    }

    private void validateMustReadOrWrite() throws ModbusConfigurationException {
        if (!isReadEnabled && !isWriteEnabled) {
            throw new ModbusConfigurationException("Should try to read or write data!");
        }
    }

    private void validateAndParseReadParameters(ModbusDataConfiguration config) throws ModbusConfigurationException {
        ModbusReadFunctionCode functionCode = this.functionCode;
        boolean readingDiscreteOrCoil = functionCode == ModbusReadFunctionCode.READ_COILS
                || functionCode == ModbusReadFunctionCode.READ_INPUT_DISCRETES;
        boolean readStartMissing = StringUtils.isBlank(config.getReadStart());
        boolean readValueTypeMissing = StringUtils.isBlank(config.getReadValueType());

        if (childOfEndpoint && readRequest == null) {
            if (!readStartMissing || !readValueTypeMissing) {
                String errmsg = String.format(
                        "Thing %s readStart=%s, and readValueType=%s were specified even though the data thing is child of endpoint (that is, write-only)!",
                        getThing().getUID(), config.getReadStart(), config.getReadValueType());
                throw new ModbusConfigurationException(errmsg);
            }
        }

        // we assume readValueType=bit by default if it is missing
        boolean allMissingOrAllPresent = (readStartMissing && readValueTypeMissing)
                || (!readStartMissing && (!readValueTypeMissing || readingDiscreteOrCoil));
        if (!allMissingOrAllPresent) {
            String errmsg = String.format(
                    "Thing %s readStart=%s, and readValueType=%s should be all present or all missing!",
                    getThing().getUID(), config.getReadStart(), config.getReadValueType());
            throw new ModbusConfigurationException(errmsg);
        } else if (!readStartMissing) {
            // all read values are present
            isReadEnabled = true;
            if (readingDiscreteOrCoil && readValueTypeMissing) {
                readValueType = ModbusConstants.ValueType.BIT;
            } else {
                try {
                    readValueType = ValueType.fromConfigValue(config.getReadValueType());
                } catch (IllegalArgumentException e) {
                    String errmsg = String.format("Thing %s readValueType=%s is invalid!", getThing().getUID(),
                            config.getReadValueType());
                    throw new ModbusConfigurationException(errmsg);
                }
            }

            if (readingDiscreteOrCoil && !ModbusConstants.ValueType.BIT.equals(readValueType)) {
                String errmsg = String.format(
                        "Thing %s invalid readValueType: Only readValueType='%s' (or undefined) supported with coils or discrete inputs. Value type was: %s",
                        getThing().getUID(), ModbusConstants.ValueType.BIT, config.getReadValueType());
                throw new ModbusConfigurationException(errmsg);
            }
        } else {
            isReadEnabled = false;
        }

        if (isReadEnabled) {
            String readStart = config.getReadStart();
            if (readStart == null) {
                throw new ModbusConfigurationException(
                        String.format("Thing %s invalid readStart: %s", getThing().getUID(), config.getReadStart()));
            }
            String[] readParts = readStart.split("\\.", 2);
            try {
                readIndex = Optional.of(Integer.parseInt(readParts[0]));
                if (readParts.length == 2) {
                    readSubIndex = Optional.of(Integer.parseInt(readParts[1]));
                } else {
                    readSubIndex = Optional.empty();
                }
            } catch (IllegalArgumentException e) {
                String errmsg = String.format("Thing %s invalid readStart: %s", getThing().getUID(),
                        config.getReadStart());
                throw new ModbusConfigurationException(errmsg);
            }
        }
        readTransformation = new Transformation(config.getReadTransform());
        validateReadIndex();
    }

    private void validateAndParseWriteParameters(ModbusDataConfiguration config) throws ModbusConfigurationException {
        boolean writeTypeMissing = StringUtils.isBlank(config.getWriteType());
        boolean writeStartMissing = StringUtils.isBlank(config.getWriteStart());
        boolean writeValueTypeMissing = StringUtils.isBlank(config.getWriteValueType());
        boolean writeTransformationMissing = StringUtils.isBlank(config.getWriteTransform());
        writeTransformation = new Transformation(config.getWriteTransform());
        boolean writingCoil = WRITE_TYPE_COIL.equals(config.getWriteType());
        writeParametersHavingTransformationOnly = (writeTypeMissing && writeStartMissing && writeValueTypeMissing
                && !writeTransformationMissing);
        boolean allMissingOrAllPresentOrOnlyNonDefaultTransform = //
                // read-only thing, no write specified
                (writeTypeMissing && writeStartMissing && writeValueTypeMissing)
                        // mandatory write parameters provided. With coils one can drop value type
                        || (!writeTypeMissing && !writeStartMissing && (!writeValueTypeMissing || writingCoil))
                        // only transformation provided
                        || writeParametersHavingTransformationOnly;
        if (!allMissingOrAllPresentOrOnlyNonDefaultTransform) {
            String errmsg = String.format(
                    "writeType=%s, writeStart=%s, and writeValueType=%s should be all present, or all missing! Alternatively, you can provide just writeTransformation, and use transformation returning JSON.",
                    config.getWriteType(), config.getWriteStart(), config.getWriteValueType());
            throw new ModbusConfigurationException(errmsg);
        } else if (!writeTypeMissing || writeParametersHavingTransformationOnly) {
            isWriteEnabled = true;
            // all write values are present
            if (!writeParametersHavingTransformationOnly && !WRITE_TYPE_HOLDING.equals(config.getWriteType())
                    && !WRITE_TYPE_COIL.equals(config.getWriteType())) {
                String errmsg = String.format("Invalid writeType=%s. Expecting %s or %s!", config.getWriteType(),
                        WRITE_TYPE_HOLDING, WRITE_TYPE_COIL);
                throw new ModbusConfigurationException(errmsg);
            }
            final ValueType localWriteValueType;
            if (writeParametersHavingTransformationOnly) {
                // Placeholder for further checks
                localWriteValueType = writeValueType = ModbusConstants.ValueType.INT16;
            } else if (writingCoil && writeValueTypeMissing) {
                localWriteValueType = writeValueType = ModbusConstants.ValueType.BIT;
            } else {
                try {
                    localWriteValueType = writeValueType = ValueType.fromConfigValue(config.getWriteValueType());
                } catch (IllegalArgumentException e) {
                    String errmsg = String.format("Invalid writeValueType=%s!", config.getWriteValueType());
                    throw new ModbusConfigurationException(errmsg);
                }
            }

            if (writingCoil && !ModbusConstants.ValueType.BIT.equals(localWriteValueType)) {
                String errmsg = String.format(
                        "Invalid writeValueType: Only writeValueType='%s' (or undefined) supported with coils. Value type was: %s",
                        ModbusConstants.ValueType.BIT, config.getWriteValueType());
                throw new ModbusConfigurationException(errmsg);
            } else if (!writingCoil && localWriteValueType.getBits() < 16) {
                // trying to write holding registers with < 16 bit value types. Not supported
                String errmsg = String.format(
                        "Invalid writeValueType: Only writeValueType with larger or equal to 16 bits are supported holding registers. Value type was: %s",
                        config.getWriteValueType());
                throw new ModbusConfigurationException(errmsg);
            }

            try {
                if (!writeParametersHavingTransformationOnly) {
                    String localWriteStart = config.getWriteStart();
                    if (localWriteStart == null) {
                        String errmsg = String.format("Thing %s invalid writeStart: %s", getThing().getUID(),
                                config.getWriteStart());
                        throw new ModbusConfigurationException(errmsg);
                    }
                    writeStart = Integer.parseInt(localWriteStart.trim());
                }
            } catch (IllegalArgumentException e) {
                String errmsg = String.format("Thing %s invalid writeStart: %s", getThing().getUID(),
                        config.getWriteStart());
                throw new ModbusConfigurationException(errmsg);
            }
        } else {
            isWriteEnabled = false;
        }
    }

    private void validateReadIndex() throws ModbusConfigurationException {
        @Nullable
        ModbusReadRequestBlueprint readRequest = this.readRequest;
        ValueType readValueType = this.readValueType;
        if (!readIndex.isPresent() || readRequest == null) {
            return;
        }
        assert readValueType != null;
        // bits represented by the value type, e.g. int32 -> 32
        int valueTypeBitCount = readValueType.getBits();
        int dataElementBits;
        switch (readRequest.getFunctionCode()) {
            case READ_INPUT_REGISTERS:
            case READ_MULTIPLE_REGISTERS:
                dataElementBits = 16;
                break;
            case READ_COILS:
            case READ_INPUT_DISCRETES:
                dataElementBits = 1;
                break;
            default:
                throw new IllegalStateException(readRequest.getFunctionCode().toString());
        }

        boolean bitQuery = dataElementBits == 1;
        if (bitQuery && readSubIndex.isPresent()) {
            String errmsg = String.format("readStart=X.Y is not allowed to be used with coils or discrete inputs!");
            throw new ModbusConfigurationException(errmsg);
        }

        if (valueTypeBitCount >= 16 && readSubIndex.isPresent()) {
            String errmsg = String
                    .format("readStart=X.Y is not allowed to be used with value types larger than 16bit!");
            throw new ModbusConfigurationException(errmsg);
        } else if (!bitQuery && valueTypeBitCount < 16 && !readSubIndex.isPresent()) {
            String errmsg = String.format("readStart=X.Y must be used with value types less than 16bit!");
            throw new ModbusConfigurationException(errmsg);
        } else if (readSubIndex.isPresent() && (readSubIndex.get() + 1) * valueTypeBitCount > 16) {
            // the sub index Y (in X.Y) is above the register limits
            String errmsg = String.format("readStart=X.Y, the value Y is too large");
            throw new ModbusConfigurationException(errmsg);
        }

        // Determine bit positions polled, both start and end inclusive
        int pollStartBitIndex = readRequest.getReference() * dataElementBits;
        int pollEndBitIndex = pollStartBitIndex + readRequest.getDataLength() * dataElementBits;

        // Determine bit positions read, both start and end inclusive
        int readStartBitIndex = readIndex.get() * dataElementBits + readSubIndex.orElse(0) * valueTypeBitCount;
        int readEndBitIndex = readStartBitIndex + valueTypeBitCount - 1;

        if (readStartBitIndex < pollStartBitIndex || readEndBitIndex > pollEndBitIndex) {
            String errmsg = String.format(
                    "Out-of-bounds: Poller is reading from index %d to %d (inclusive) but this thing configured to read '%s' starting from element %d. Exceeds polled data bounds.",
                    pollStartBitIndex / dataElementBits, pollEndBitIndex / dataElementBits, readValueType,
                    readIndex.get());
            throw new ModbusConfigurationException(errmsg);
        }
    }

    private boolean containsOnOff(List<Class<? extends State>> channelAcceptedDataTypes) {
        return channelAcceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OnOffType.class);
        });
    }

    private boolean containsOpenClosed(List<Class<? extends State>> acceptedDataTypes) {
        return acceptedDataTypes.stream().anyMatch(clz -> {
            return clz.equals(OpenClosedType.class);
        });
    }

    public synchronized void onReadResult(AsyncModbusReadResult result) {
        result.getRegisters().ifPresent(registers -> onRegisters(result.getRequest(), registers));
        result.getBits().ifPresent(bits -> onBits(result.getRequest(), bits));
    }

    public synchronized void handleReadError(AsyncModbusFailure<ModbusReadRequestBlueprint> failure) {
        onError(failure.getRequest(), failure.getCause());
    }

    public synchronized void handleWriteError(AsyncModbusFailure<ModbusWriteRequestBlueprint> failure) {
        onError(failure.getRequest(), failure.getCause());
    }

    private synchronized void onRegisters(ModbusReadRequestBlueprint request, ModbusRegisterArray registers) {
        if (hasConfigurationError()) {
            return;
        } else if (!isReadEnabled) {
            return;
        }
        ValueType readValueType = this.readValueType;
        if (readValueType == null) {
            return;
        }
        State numericState;

        // extractIndex:
        // e.g. with bit, extractIndex=4 means 5th bit (from right) ("10.4" -> 5th bit of register 10, "10.4" -> 5th bit
        // of register 10)
        // bit of second register)
        // e.g. with 8bit integer, extractIndex=3 means high byte of second register
        //
        // with <16 bit types, this is the index of the N'th 1-bit/8-bit item. Each register has 16/2 items,
        // respectively.
        // with >=16 bit types, this is index of first register
        int extractIndex;
        if (readValueType.getBits() >= 16) {
            // Invariant, checked in initialize
            assert readSubIndex.orElse(0) == 0;
            extractIndex = readIndex.get() - pollStart;
        } else {
            int subIndex = readSubIndex.orElse(0);
            int itemsPerRegister = 16 / readValueType.getBits();
            extractIndex = (readIndex.get() - pollStart) * itemsPerRegister + subIndex;
        }
        numericState = ModbusBitUtilities.extractStateFromRegisters(registers, extractIndex, readValueType)
                .map(state -> (State) state).orElse(UnDefType.UNDEF);
        boolean boolValue = !numericState.equals(DecimalType.ZERO);
        Map<ChannelUID, State> values = processUpdatedValue(numericState, boolValue);
        logger.debug(
                "Thing {} channels updated: {}. readValueType={}, readIndex={}, readSubIndex(or 0)={}, extractIndex={} -> numeric value {} and boolValue={}. Registers {} for request {}",
                thing.getUID(), values, readValueType, readIndex, readSubIndex.orElse(0), extractIndex, numericState,
                boolValue, registers, request);
    }

    private synchronized void onBits(ModbusReadRequestBlueprint request, BitArray bits) {
        if (hasConfigurationError()) {
            return;
        } else if (!isReadEnabled) {
            return;
        }
        boolean boolValue = bits.getBit(readIndex.get() - pollStart);
        DecimalType numericState = boolValue ? new DecimalType(BigDecimal.ONE) : DecimalType.ZERO;
        Map<ChannelUID, State> values = processUpdatedValue(numericState, boolValue);
        logger.debug(
                "Thing {} channels updated: {}. readValueType={}, readIndex={} -> numeric value {} and boolValue={}. Bits {} for request {}",
                thing.getUID(), values, readValueType, readIndex, numericState, boolValue, bits, request);
    }

    private synchronized void onError(ModbusReadRequestBlueprint request, Exception error) {
        if (hasConfigurationError()) {
            return;
        } else if (!isReadEnabled) {
            return;
        }
        if (error instanceof ModbusConnectionException) {
            logger.error("Thing {} '{}' had {} error on read: {}", getThing().getUID(), getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else if (error instanceof ModbusTransportException) {
            logger.error("Thing {} '{}' had {} error on read: {}", getThing().getUID(), getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else {
            logger.error(
                    "Thing {} '{}' had {} error on read: {} (message: {}). Stack trace follows since this is unexpected error.",
                    getThing().getUID(), getThing().getLabel(), error.getClass().getName(), error.toString(),
                    error.getMessage(), error);
        }
        Map<ChannelUID, State> states = new HashMap<>();
        ChannelUID lastReadErrorUID = getChannelUID(ModbusBindingConstantsInternal.CHANNEL_LAST_READ_ERROR);
        if (isLinked(lastReadErrorUID)) {
            states.put(lastReadErrorUID, new DateTimeType());
        }

        synchronized (this) {
            // Update channels
            states.forEach((uid, state) -> {
                tryUpdateState(uid, state);
            });

            updateStatusIfChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error (%s) with read. Request: %s. Description: %s. Message: %s",
                            error.getClass().getSimpleName(), request, error.toString(), error.getMessage()));
        }
    }

    private synchronized void onError(ModbusWriteRequestBlueprint request, Exception error) {
        if (hasConfigurationError()) {
            return;
        } else if (!isWriteEnabled) {
            return;
        }
        if (error instanceof ModbusConnectionException) {
            logger.error("Thing {} '{}' had {} error on write: {}", getThing().getUID(), getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else if (error instanceof ModbusTransportException) {
            logger.error("Thing {} '{}' had {} error on write: {}", getThing().getUID(), getThing().getLabel(),
                    error.getClass().getSimpleName(), error.toString());
        } else {
            logger.error(
                    "Thing {} '{}' had {} error on write: {} (message: {}). Stack trace follows since this is unexpected error.",
                    getThing().getUID(), getThing().getLabel(), error.getClass().getName(), error.toString(),
                    error.getMessage(), error);
        }
        Map<ChannelUID, State> states = new HashMap<>();
        ChannelUID lastWriteErrorUID = getChannelUID(ModbusBindingConstantsInternal.CHANNEL_LAST_WRITE_ERROR);
        if (isLinked(lastWriteErrorUID)) {
            states.put(lastWriteErrorUID, new DateTimeType());
        }

        synchronized (this) {
            // Update channels
            states.forEach((uid, state) -> {
                tryUpdateState(uid, state);
            });

            updateStatusIfChanged(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    String.format("Error (%s) with write. Request: %s. Description: %s. Message: %s",
                            error.getClass().getSimpleName(), request, error.toString(), error.getMessage()));
        }
    }

    public synchronized void onWriteResponse(AsyncModbusWriteResult result) {
        if (hasConfigurationError()) {
            return;
        } else if (!isWriteEnabled) {
            return;
        }
        logger.debug("Successful write, matching request {}", result.getRequest());
        updateStatusIfChanged(ThingStatus.ONLINE);
        ChannelUID lastWriteSuccessUID = getChannelUID(ModbusBindingConstantsInternal.CHANNEL_LAST_WRITE_SUCCESS);
        if (isLinked(lastWriteSuccessUID)) {
            updateState(lastWriteSuccessUID, new DateTimeType());
        }
    }

    /**
     * Update linked channels
     *
     * @param numericState numeric state corresponding to polled data (or UNDEF with floating point NaN or infinity)
     * @param boolValue boolean value corresponding to polled data
     * @return updated channel data
     */
    private Map<ChannelUID, State> processUpdatedValue(State numericState, boolean boolValue) {
        Transformation localReadTransformation = readTransformation;
        if (localReadTransformation == null) {
            // We should always have transformation available if thing is initalized properly
            logger.trace("No transformation available, aborting processUpdatedValue");
            return Collections.emptyMap();
        }
        Map<ChannelUID, State> states = new HashMap<>();
        CHANNEL_ID_TO_ACCEPTED_TYPES.keySet().stream().forEach(channelId -> {
            ChannelUID channelUID = getChannelUID(channelId);
            if (!isLinked(channelUID)) {
                return;
            }
            List<Class<? extends State>> acceptedDataTypes = CHANNEL_ID_TO_ACCEPTED_TYPES.get(channelId);
            if (acceptedDataTypes.isEmpty()) {
                return;
            }

            State boolLikeState;
            if (containsOnOff(acceptedDataTypes)) {
                boolLikeState = boolValue ? OnOffType.ON : OnOffType.OFF;
            } else if (containsOpenClosed(acceptedDataTypes)) {
                boolLikeState = boolValue ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
            } else {
                boolLikeState = null;
            }

            State transformedState;
            if (localReadTransformation.isIdentityTransform()) {
                if (boolLikeState != null) {
                    // A bit of smartness for ON/OFF and OPEN/CLOSED with boolean like items
                    transformedState = boolLikeState;
                } else {
                    // Numeric states always go through transformation. This allows value of 17.5 to be
                    // converted to
                    // 17.5% with percent types (instead of raising error)
                    transformedState = localReadTransformation.transformState(bundleContext, acceptedDataTypes,
                            numericState);
                }
            } else {
                transformedState = localReadTransformation.transformState(bundleContext, acceptedDataTypes,
                        numericState);
            }

            if (transformedState != null) {
                logger.trace(
                        "Channel {} will be updated to '{}' (type {}). Input data: number value {} (value type '{}' taken into account) and bool value {}. Transformation: {}",
                        channelId, transformedState, transformedState.getClass().getSimpleName(), numericState,
                        readValueType, boolValue,
                        localReadTransformation.isIdentityTransform() ? "<identity>" : localReadTransformation);
                states.put(channelUID, transformedState);
            } else {
                String types = StringUtils.join(acceptedDataTypes.stream().map(cls -> cls.getSimpleName()).toArray(),
                        ", ");
                logger.warn(
                        "Channel {} will not be updated since transformation was unsuccessful. Channel is expecting the following data types [{}]. Input data: number value {} (value type '{}' taken into account) and bool value {}. Transformation: {}",
                        channelId, types, numericState, readValueType, boolValue,
                        localReadTransformation.isIdentityTransform() ? "<identity>" : localReadTransformation);
            }
        });

        ChannelUID lastReadSuccessUID = getChannelUID(ModbusBindingConstantsInternal.CHANNEL_LAST_READ_SUCCESS);
        if (isLinked(lastReadSuccessUID)) {
            states.put(lastReadSuccessUID, new DateTimeType());
        }
        updateExpiredChannels(states);
        return states;
    }

    private void updateExpiredChannels(Map<ChannelUID, State> states) {
        synchronized (this) {
            updateStatusIfChanged(ThingStatus.ONLINE);
            long now = System.currentTimeMillis();
            // Update channels that have not been updated in a while, or when their values has changed
            states.forEach((uid, state) -> updateExpiredChannel(now, uid, state));
            channelLastState = states;
        }
    }

    // since lastState can be null, and "lastState == null" in conditional is not useless
    @SuppressWarnings("null")
    private void updateExpiredChannel(long now, ChannelUID uid, State state) {
        @Nullable
        State lastState = channelLastState.get(uid);
        long lastUpdatedMillis = channelLastUpdated.getOrDefault(uid, 0L);
        long millisSinceLastUpdate = now - lastUpdatedMillis;
        if (lastUpdatedMillis <= 0L || lastState == null || updateUnchangedValuesEveryMillis <= 0L
                || millisSinceLastUpdate > updateUnchangedValuesEveryMillis || !lastState.equals(state)) {
            tryUpdateState(uid, state);
            channelLastUpdated.put(uid, now);
        }
    }

    private void tryUpdateState(ChannelUID uid, State state) {
        try {
            updateState(uid, state);
        } catch (IllegalArgumentException e) {
            logger.warn("Error updating state '{}' (type {}) to channel {}: {} {}", state,
                    Optional.ofNullable(state).map(s -> s.getClass().getName()).orElse("null"), uid,
                    e.getClass().getName(), e.getMessage());
        }
    }

    private ChannelUID getChannelUID(String channelID) {
        return channelCache.computeIfAbsent(channelID, id -> new ChannelUID(getThing().getUID(), id));
    }

    private void updateStatusIfChanged(ThingStatus status) {
        updateStatusIfChanged(status, ThingStatusDetail.NONE, null);
    }

    private void updateStatusIfChanged(ThingStatus status, ThingStatusDetail statusDetail,
            @Nullable String description) {
        ThingStatusInfo newStatusInfo = new ThingStatusInfo(status, statusDetail, description);
        Duration durationSinceLastUpdate = Duration.between(lastStatusInfoUpdate, LocalDateTime.now());
        boolean intervalElapsed = MIN_STATUS_INFO_UPDATE_INTERVAL.minus(durationSinceLastUpdate).isNegative();
        if (statusInfo.getStatus() == ThingStatus.UNKNOWN || !statusInfo.equals(newStatusInfo) || intervalElapsed) {
            statusInfo = newStatusInfo;
            lastStatusInfoUpdate = LocalDateTime.now();
            updateStatus(newStatusInfo);
        }
    }

    /**
     * Update status using pre-constructed ThingStatusInfo
     *
     * Implementation adapted from BaseThingHandler updateStatus implementations
     *
     * @param statusInfo new status info
     */
    protected void updateStatus(ThingStatusInfo statusInfo) {
        synchronized (this) {
            ThingHandlerCallback callback = getCallback();
            if (callback != null) {
                callback.statusUpdated(this.thing, statusInfo);
            } else {
                logger.warn("Handler {} tried updating the thing status although the handler was already disposed.",
                        this.getClass().getSimpleName());
            }
        }
    }
}
