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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BruntHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Kyle Cooke - Initial contribution
 */
@NonNullByDefault
public class BruntBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(BruntBridgeHandler.class);

    private @Nullable BruntConfiguration config;
    private final HttpClient httpClient;
    private final String loginUrl = "https://sky.brunt.co/session";
    private final String thingListUrl = "https://sky.brunt.co/thing";
    private final String baseThingManagementUrl = "https://thing.brunt.co:8080/thing";
    private static final JsonParser jsonParser = new JsonParser();

    public BruntBridgeHandler(Bridge thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            authenticate();
        }
        logger.debug("The brunt account binding is a read-only binding and cannot handle command '{}'.", command);
    }

    @Override
    public void initialize() {
        // logger.debug("Start initializing!");
        config = getConfigAs(BruntConfiguration.class);

        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            String problem = "error unset";
            ThingStatusDetail error_type = ThingStatusDetail.CONFIGURATION_ERROR;
            boolean thingReachable = false; // <background task with long running initialization here>
            if (config.password != null && config.username != null){
                Boolean authentication_result = authenticate();
                if (authentication_result == null){
                    return;
                }
                if (authentication_result){
                    thingReachable = true;
                } else {
                    problem = "Bad login details";
                }
            } else {
                problem = "incomplete login details provided";
            }
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE, error_type, problem);
            }
        });

        logger.debug("Finished initializing!");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    private @Nullable Boolean authenticate(){
        String login_data = String.format("{\"ID\": \"%s\", \"PASS\": \"%s\"}", config.username, config.password);
        try {
            ContentResponse response = httpClient.newRequest(loginUrl).method(HttpMethod.POST)
                    .content(new StringContentProvider(login_data), "application/x-www-form-urlencoded")
                    .header(HttpHeader.ACCEPT, "application/vnd.brunt.v1+json")
                    .header(HttpHeader.ACCEPT_LANGUAGE, "en-gb")
                    .header(HttpHeader.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216")
                    .send();
            return response.getStatus() == HttpStatus.OK_200;
        } catch (InterruptedException e) {
            logger.error("login interrupted");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "login interrupted");
        } catch (TimeoutException e) {
            logger.error("login timed out");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "login timed out");
        } catch (ExecutionException e) {
            logger.error("login failed");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "login failed");
        }
        return null;
    }

    public boolean checkDeviceAccess(String uri) throws InterruptedException, ExecutionException, TimeoutException {
        authenticate();
        ContentResponse response = httpClient.newRequest(baseThingManagementUrl + uri).method(HttpMethod.GET)
                .header(HttpHeader.ACCEPT, "application/vnd.brunt.v1+json")
                .header(HttpHeader.ACCEPT_LANGUAGE, "en-gb")
                .header(HttpHeader.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216")
                .send();
        return response.getStatus() == HttpStatus.OK_200;
    }

    public boolean setPosition(String uri, int value){
        authenticate();
        if (0 > value || value > 100){
            return false;
        }
        String login_data = String.format("{\"requestPosition\": \"%d\"}", value);
        try {
            ContentResponse response = httpClient.newRequest(baseThingManagementUrl + uri).method(HttpMethod.PUT)
                    .content(new StringContentProvider(login_data), "application/json")
                    .header(HttpHeader.ACCEPT, "application/vnd.brunt.v1+json")
                    .header(HttpHeader.ACCEPT_LANGUAGE, "en-gb")
                    .header(HttpHeader.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E216")
                    .send();
            return response.getStatus() == HttpStatus.OK_200;
        } catch (InterruptedException e) {
            logger.error("connection interrupted");
        } catch (TimeoutException e) {
            logger.error("connection timed out");
        } catch (ExecutionException e) {
            logger.error("connection failed");
        }
        return false;
    }
}
