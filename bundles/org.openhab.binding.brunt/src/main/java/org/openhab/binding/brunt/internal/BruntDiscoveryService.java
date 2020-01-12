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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;

import java.util.Collections;

import static org.openhab.binding.brunt.internal.BruntBindingConstants.THING_TYPE_BLIND_ENGINE;

/**
 * The {@link BruntDiscoveryService} is responsible for finding new things
 *
 * @author Kyle Cooke - Initial contribution
 */
public class BruntDiscoveryService extends AbstractDiscoveryService {

    private final BruntBridgeHandler bridge;
    private static final JsonParser jsonParser = new JsonParser();

    public BruntDiscoveryService(BruntBridgeHandler bridge) throws IllegalArgumentException {
        super(Collections.singleton(THING_TYPE_BLIND_ENGINE), 30, false);
        this.bridge = bridge;
    }

    @Override
    protected void startScan() {
        String result = bridge.getDevicesString();
        if (result.length() > 0) {
            JsonElement parsed = jsonParser.parse(result);
            if (parsed.isJsonArray()){
                for (JsonElement e : parsed.getAsJsonArray()) {
                    JsonObject object = e.getAsJsonObject();
                    ThingUID uid = new ThingUID(THING_TYPE_BLIND_ENGINE, bridge.getThing().getUID(), object.get("SERIAL").getAsString());
                    DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(uid)
                            .withThingType(THING_TYPE_BLIND_ENGINE)
                            .withBridge(bridge.getThing().getUID())
                            .withProperty("uri", object.get("thingUri").getAsString())
                            .withLabel(object.get("NAME").getAsString()).build();

                    thingDiscovered(discoveryResult);
                }
            }
        }
    }
}
