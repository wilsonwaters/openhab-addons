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
package org.openhab.binding.squeezebox.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.binding.BaseDynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.thing.i18n.ChannelTypeI18nLocalizationService;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Dynamic provider of state options while leaving other state description fields as original.
 *
 * @author Gregory Moyer - Initial contribution
 * @author Mark Hilbush - Adapted to squeezebox binding
 */
@Component(service = { DynamicStateDescriptionProvider.class, SqueezeBoxStateDescriptionOptionsProvider.class })
@NonNullByDefault
public class SqueezeBoxStateDescriptionOptionsProvider extends BaseDynamicStateDescriptionProvider {

    @Activate
    public SqueezeBoxStateDescriptionOptionsProvider(
            @Reference ChannelTypeI18nLocalizationService channelTypeI18nLocalizationService) {
        this.channelTypeI18nLocalizationService = channelTypeI18nLocalizationService;
    }
}
