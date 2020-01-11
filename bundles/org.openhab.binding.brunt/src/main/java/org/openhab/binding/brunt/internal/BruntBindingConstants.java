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
package org.openhab.binding.brunt.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link BruntBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Kyle Cooke - Initial contribution
 */
@NonNullByDefault
public class BruntBindingConstants {

    private static final String BINDING_ID = "brunt";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "brunt-bridge");
    public static final ThingTypeUID THING_TYPE_BLIND_ENGINE = new ThingTypeUID(BINDING_ID, "brunt-blind-engine");

    // List of all Channel ids
    public static final String CHANNEL_POSITION = "position";
}
