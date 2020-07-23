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
package org.openhab.binding.miio.internal;

/**
 * The {@link MiIoBindingConfiguration} class defines variables which are
 * used for the binding configuration.
 *
 * @author Marcel Verpaalen - Initial contribution
 */
@SuppressWarnings("null")
public final class MiIoBindingConfiguration {
    public String host;
    public String token;
    public String deviceId;
    public String model;
    public int refreshInterval;
    public int timeout;
    public String cloudServer;
}
