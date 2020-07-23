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
package org.openhab.binding.amazonechocontrol.internal.jsons;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link JsonActivity} encapsulate the GSON data of the sequence command AlexaAnnouncement for sending
 * announcements
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
public class JsonAnnouncementContent {

    public String locale = "";
    public final Display display = new Display();
    public final Speak speak = new Speak();

    public static class Display {
        public @Nullable String title;
        public @Nullable String body;
    }

    public static class Speak {
        public String type = "text";
        public @Nullable String value;
    }
}
