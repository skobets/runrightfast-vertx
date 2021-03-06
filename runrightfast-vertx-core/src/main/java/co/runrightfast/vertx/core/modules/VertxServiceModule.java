/*
 Copyright 2015 Alfio Zappala

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package co.runrightfast.vertx.core.modules;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.vertx.core.VertxService;
import co.runrightfast.vertx.core.impl.VertxServiceImpl;
import co.runrightfast.vertx.core.inject.qualifiers.VertxServiceConfig;
import co.runrightfast.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import com.typesafe.config.Config;
import dagger.Module;
import dagger.Provides;
import java.util.Set;
import javax.inject.Singleton;

/**
 *
 * @author alfio
 */
@Module
public class VertxServiceModule {

    @Provides
    @Singleton
    public VertxService vertxService(@VertxServiceConfig final Config config, final Set<RunRightFastVerticleDeployment> deployments, final AppEventLogger appEventLogger) {
        final VertxService service = new VertxServiceImpl(config, deployments, appEventLogger);
        ServiceUtils.start(service);
        return service;
    }
}
