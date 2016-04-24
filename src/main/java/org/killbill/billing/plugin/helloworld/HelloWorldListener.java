/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2015 Groupon, Inc
 * Copyright 2014-2015 The Billing Project, LLC
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

package org.killbill.billing.plugin.helloworld;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.api.notification.PluginConfigurationHandler;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.service.log.LogService;

import javax.annotation.Nullable;
import java.util.Properties;
import java.util.UUID;

public class HelloWorldListener extends PluginConfigurationEventHandler implements OSGIKillbillEventHandler {

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;

    public HelloWorldListener(final OSGIKillbillLogService logService, final OSGIKillbillAPI killbillAPI) {
        super(new HelloWorldPluginConfigurationHandler(HelloWorldActivator.PLUGIN_NAME, killbillAPI, logService));
        this.logService = logService;
        this.osgiKillbillAPI = killbillAPI;
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {

        logService.log(LogService.LOG_INFO, "Received event " + killbillEvent.getEventType() +
                " for object id " + killbillEvent.getObjectId() +
                " of type " + killbillEvent.getObjectType());

        switch (killbillEvent.getEventType()) {
            //
            // Calls java plugin framework PluginConfigurationEventHandler to handle TENANT_CONFIG_CHANGE/TENANT_CONFIG_DELETION events
            //
            case TENANT_CONFIG_CHANGE:
            case TENANT_CONFIG_DELETION:
                super.handleKillbillEvent(killbillEvent);
                break;

            //
            // Handle ACCOUNT_CREATION and ACCOUNT_CHANGE (only) for demo purpose and just print the account
            //
            case ACCOUNT_CREATION:
            case ACCOUNT_CHANGE:
                try {
                    final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), new HelloWorldContext(killbillEvent.getTenantId()));
                    logService.log(LogService.LOG_INFO, "Account information: " + account);
                } catch (final AccountApiException e) {
                    logService.log(LogService.LOG_WARNING, "Unable to find account", e);
                }
                break;

            // Nothing
            default:
                break;

        }
    }

    private static class HelloWorldPluginConfigurationHandler extends PluginConfigurationHandler {

        private final LogService logService;

        public HelloWorldPluginConfigurationHandler(String pluginName, OSGIKillbillAPI osgiKillbillAPI, OSGIKillbillLogService osgiKillbillLogService) {
            super(pluginName, osgiKillbillAPI, osgiKillbillLogService);
            this.logService = osgiKillbillLogService;
        }

        @Override
        protected void configure(@Nullable UUID kbTenantId) {
            final Properties properties = getTenantConfigurationAsProperties(kbTenantId);
            if (properties == null) {
                // Invalid configuration or tenant not configured, we will default to the global configurable (or previous configuration)
                return;
            }

            logService.log(LogService.LOG_INFO, String.format("Properties for tenant='%s': properties='%s'",
                    kbTenantId, properties.stringPropertyNames()));
        }
    }

    private static final class HelloWorldContext implements TenantContext {

        private final UUID tenantId;

        private HelloWorldContext(final UUID tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public UUID getTenantId() {
            return tenantId;
        }
    }
}
