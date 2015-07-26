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
package co.runrightfast.vertx.core.components;

import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.modules.ApplicationConfigModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import com.codahale.metrics.MetricFilter;
import com.typesafe.config.ConfigFactory;
import dagger.Component;
import io.vertx.core.Vertx;
import javax.inject.Singleton;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class RunRightFastVertxApplicationTest {

    private static VertxService vertxService;

    @Component(
            modules = {
                ApplicationConfigModule.class,
                VertxServiceModule.class
            }
    )
    @Singleton
    public static interface TestApp extends RunRightFastVertxApplication {
    }

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("config.resource", String.format("%s.conf", RunRightFastVertxApplicationTest.class.getSimpleName()));
        ConfigFactory.invalidateCaches();
        metricRegistry.removeMatching(MetricFilter.ALL);
        vertxService = DaggerRunRightFastVertxApplicationTest_TestApp.create().vertxService();
    }

    @AfterClass
    public static void tearDownClass() {
        ServiceUtils.stop(vertxService);
    }

    @Test
    public void test_vertx_default_options() {
        log.info("test_vertx_default_options");
        final Vertx vertx = vertxService.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));
    }

}
