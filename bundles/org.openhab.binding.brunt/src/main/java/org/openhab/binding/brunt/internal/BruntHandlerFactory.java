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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.i18n.LocationProvider;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openhab.binding.brunt.internal.BruntBindingConstants.THING_TYPE_BLIND_ENGINE;
import static org.openhab.binding.brunt.internal.BruntBindingConstants.THING_TYPE_BRIDGE;

/**
 * The {@link BruntHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Kyle Cooke - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.brunt", service = ThingHandlerFactory.class)
public class BruntHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(
            Stream.of(THING_TYPE_BRIDGE, THING_TYPE_BLIND_ENGINE).collect(Collectors.toSet())
    );
    private final HttpClient httpClient;
    private final Map<ThingUID, @Nullable ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Activate
    public BruntHandlerFactory(final @Reference HttpClientFactory httpClientFactory,
                                 final @Reference LocaleProvider localeProvider, final @Reference LocationProvider locationProvider,
                                 final @Reference TranslationProvider i18nProvider) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_BRIDGE.equals(thingTypeUID)) {
            BruntBridgeHandler handler = new BruntBridgeHandler((Bridge) thing, httpClient);
            registerDiscoveryService(handler);
            return handler;
        } else if (THING_TYPE_BLIND_ENGINE.equals(thingTypeUID)){
            return new BruntBlindHandler(thing);
        }

        return null;
    }

    private synchronized void registerDiscoveryService(BruntBridgeHandler bridgeHandler) {
        BruntDiscoveryService discoveryService = new BruntDiscoveryService(bridgeHandler);
        //discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof BruntBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                BruntDiscoveryService service = (BruntDiscoveryService) bundleContext
                        .getService(serviceReg.getReference());
                serviceReg.unregister();
                //if (service != null) {
                //    service.deactivate();
                //}
            }
        }
    }
}
