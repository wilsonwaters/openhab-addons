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
package org.openhab.binding.bluetooth.bluegiga.internal.command.system;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.bluetooth.bluegiga.internal.BlueGigaCommand;

/**
 * Class to implement the BlueGiga command <b>reset</b>.
 * <p>
 * This command resets the local device immediately. The command does not have a response.
 * <p>
 * This class provides methods for processing BlueGiga API commands.
 * <p>
 * Note that this code is autogenerated. Manual changes may be overwritten.
 *
 * @author Chris Jackson - Initial contribution of Java code generator
 */
@NonNullByDefault
public class BlueGigaResetCommand extends BlueGigaCommand {
    public static int COMMAND_CLASS = 0x00;
    public static int COMMAND_METHOD = 0x00;

    /**
     * Selects the boot mode. 0 : boot to main program. 1 : boot to DFU
     * <p>
     * BlueGiga API type is <i>boolean</i> - Java type is {@link boolean}
     */
    private boolean bootInDfu = false;

    /**
     * Selects the boot mode. 0 : boot to main program. 1 : boot to DFU
     *
     * @param bootInDfu the bootInDfu to set as {@link boolean}
     */
    public void setBootInDfu(boolean bootInDfu) {
        this.bootInDfu = bootInDfu;
    }

    @Override
    public int[] serialize() {
        // Serialize the header
        serializeHeader(COMMAND_CLASS, COMMAND_METHOD);

        // Serialize the fields
        serializeBoolean(bootInDfu);

        return getPayload();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BlueGigaResetCommand [bootInDfu=");
        builder.append(bootInDfu);
        builder.append(']');
        return builder.toString();
    }
}
