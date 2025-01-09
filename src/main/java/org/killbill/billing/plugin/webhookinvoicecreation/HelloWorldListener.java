/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.webhookinvoicecreation;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.formatters.InvoiceFormatter;
import org.killbill.billing.invoice.api.formatters.InvoiceItemFormatter;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.notification.plugin.api.ExtBusEventType;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HelloWorldListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldListener.class);

    private final OSGIKillbillAPI osgiKillbillAPI;

    private final ScheduledExecutorService executorService;
    private final ObjectMapper objectMapper;

    private static final String NOTIFICATION_ENDPOINT = "https://webhook-test.com/9d0952fb6aa386afc725966ba19fb6aa";


    public HelloWorldListener(final OSGIKillbillAPI killbillAPI) {
        this.osgiKillbillAPI = killbillAPI;
        this.executorService = Executors.newScheduledThreadPool(1);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logger.info("Received event {} for object id {} of type {}",
                    killbillEvent.getEventType(),
                    killbillEvent.getObjectId(),
                    killbillEvent.getObjectType());

        if (!ExtBusEventType.INVOICE_CREATION.equals(killbillEvent.getEventType())) {
            return; // Ignorar eventos que no sean de creación de factura
        }

        try {
            final String payload = objectMapper.writeValueAsString(killbillEvent);
            sendNotification(payload, 0);
        } catch (final Exception e) {
            logger.error( "Error serializing event payload", e);
        }
    }

    private void sendNotification(String payload, int retryCount) {
        executorService.submit(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(NOTIFICATION_ENDPOINT).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.getOutputStream().write(payload.getBytes());

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    logger.info("Notification sent successfully");
                } else {
                    logger.info("implementar reintento");
//                    scheduleRetry(payload, retryCount + 1);
                }
            } catch (final Exception e) {
                logger.error( "Failed to send notification", e);
                logger.info("implementar reintento");
//                scheduleRetry(payload, retryCount + 1);
            }
        });
    }
}
