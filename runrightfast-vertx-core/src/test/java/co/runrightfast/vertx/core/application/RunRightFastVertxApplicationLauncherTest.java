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
package co.runrightfast.vertx.core.application;

import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.application.jmx.ApplicationMXBean;
import co.runrightfast.vertx.core.components.DaggerRunRightFastVertxApplicationTest_TestApp;
import co.runrightfast.vertx.core.components.RunRightFastVertxApplication;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.modules.ApplicationConfigModule;
import co.runrightfast.vertx.core.modules.VertxServiceModule;
import co.runrightfast.vertx.core.utils.JmxUtils;
import static co.runrightfast.vertx.core.utils.JmxUtils.applicationMBeanObjectName;
import co.runrightfast.vertx.core.utils.JsonUtils;
import co.runrightfast.vertx.core.utils.ProtobufUtils;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import com.codahale.metrics.MetricFilter;
import com.typesafe.config.ConfigFactory;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import javax.inject.Singleton;
import javax.management.JMX;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
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
public class RunRightFastVertxApplicationLauncherTest {

    static class TestVerticle extends RunRightFastVerticle {

        @Getter
        private final RunRightFastVerticleId runRightFastVerticleId
                = RunRightFastVerticleId.builder()
                .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
                .name(getClass().getSimpleName())
                .version("1.0.0")
                .build();

        @Override
        protected void startUp() {
        }

        @Override
        protected void shutDown() {
        }

    }

    @Module
    static class RunRightFastVerticleDeploymentModule {

        @Provides(type = Provides.Type.SET)
        @Singleton
        public RunRightFastVerticleDeployment provideTestVerticleRunRightFastVerticleDeployment() {
            return RunRightFastVerticleDeployment.builder()
                    .deploymentOptions(new DeploymentOptions())
                    .verticle(new RunRightFastVertxApplicationLauncherTest.TestVerticle())
                    .build();
        }
    }

    @Component(
            modules = {
                ApplicationConfigModule.class,
                VertxServiceModule.class,
                RunRightFastVerticleDeploymentModule.class
            }
    )
    @Singleton
    public static interface TestApp extends RunRightFastVertxApplication {
    }

    private static RunRightFastVertxApplication app;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("config.resource", String.format("%s.conf", RunRightFastVertxApplicationLauncherTest.class.getSimpleName()));
        ConfigFactory.invalidateCaches();
        metricRegistry.removeMatching(MetricFilter.ALL);
        app = DaggerRunRightFastVertxApplicationTest_TestApp.create();

    }

    @AfterClass
    public static void tearDownClass() {
        ServiceUtils.stop(app.vertxService());
    }

    /**
     * Test of run method, of class RunRightFastVertxApplicationLauncher.
     */
    @Test
    public void testHelpOption() {
        System.out.println(String.format("\n\n%s%s%s%s%s", StringUtils.repeat('*', 10), StringUtils.repeat(' ', 3), "testHelpOption -h", StringUtils.repeat(' ', 3), StringUtils.repeat('*', 10)));
        RunRightFastVertxApplicationLauncher.run(() -> app, "-h");
        System.out.println(String.format("\n\n%s%s%s%s%s", StringUtils.repeat('*', 10), StringUtils.repeat(' ', 3), "testHelpOption --help", StringUtils.repeat(' ', 3), StringUtils.repeat('*', 10)));
        RunRightFastVertxApplicationLauncher.run(() -> app, "--help");
    }

    /**
     * Test of run method, of class RunRightFastVertxApplicationLauncher.
     */
    @Test
    public void testVersionOption() {
        System.out.println(String.format("\n\n%s%s%s%s%s", StringUtils.repeat('*', 10), StringUtils.repeat(' ', 3), "testVersionOption -v", StringUtils.repeat(' ', 3), StringUtils.repeat('*', 10)));
        RunRightFastVertxApplicationLauncher.run(() -> app, "-v");
        System.out.println(String.format("\n\n%s%s%s%s%s", StringUtils.repeat('*', 10), StringUtils.repeat(' ', 3), "testVersionOption --version", StringUtils.repeat(' ', 3), StringUtils.repeat('*', 10)));
        RunRightFastVertxApplicationLauncher.run(() -> app, "--version");
    }

    /**
     * Test of run method, of class RunRightFastVertxApplicationLauncher.
     */
    @Test
    public void testConfigOption() {
        System.out.println(String.format("\n\n%s%s%s%s%s", StringUtils.repeat('*', 10), StringUtils.repeat(' ', 3), "testConfigOption -v", StringUtils.repeat(' ', 3), StringUtils.repeat('*', 10)));
        RunRightFastVertxApplicationLauncher.run(() -> app, "-c");
        System.out.println(String.format("\n\n%s%s%s%s%s", StringUtils.repeat('*', 10), StringUtils.repeat(' ', 3), "testConfigOption --version", StringUtils.repeat(' ', 3), StringUtils.repeat('*', 10)));
        RunRightFastVertxApplicationLauncher.run(() -> app, "--config");
    }

    @Test
    public void testRunApp() throws Exception {
        ForkJoinPool.commonPool().execute(() -> RunRightFastVertxApplicationLauncher.run(() -> app));

        final RunRightFastVerticleId verticleManagerId = RunRightFastVerticleManager.VERTICLE_ID;
        final CompletableFuture future = new CompletableFuture();
        final String address = EventBusAddress.eventBusAddress(verticleManagerId, "get-verticle-deployments");
        final Vertx vertx = app.vertxService().getVertx();
        vertx.eventBus().send(
                address,
                GetVerticleDeployments.Request.newBuilder().build(),
                new DeliveryOptions().setSendTimeout(2000L),
                getVerticleDeploymentsResponseHandler(future)
        );
        final Object result = future.get(2000L, TimeUnit.MILLISECONDS);

        final ApplicationMXBean appMXBean = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), applicationMBeanObjectName(JmxUtils.RUNRIGHTFAST_JMX_DOMAIN, ApplicationMXBean.class), ApplicationMXBean.class);
        assertThat(appMXBean.getApplicationGroup(), is("co.runrightfast"));
        assertThat(appMXBean.getApplicationName(), is("test-app"));
        assertThat(appMXBean.getApplicationVersion(), is("1.0.0"));
        log.logp(INFO, getClass().getName(), "testRunApp", "{0}:\n{1}", new Object[]{"configAsHConf", appMXBean.configAsHConf()});
        log.logp(INFO, getClass().getName(), "testRunApp", "{0}:\n{1}", new Object[]{"configAsHJson", appMXBean.configAsJson()});
        log.logp(INFO, getClass().getName(), "testRunApp", "{0}:\n{1}", new Object[]{"configWithCommentsAndSourceInfo", appMXBean.configWithCommentsAndSourceInfo()});
        appMXBean.shutdown();
        app.vertxService().awaitTerminated();
    }

    private Handler<AsyncResult<Message<GetVerticleDeployments.Response>>> getVerticleDeploymentsResponseHandler(final CompletableFuture future) {
        return result -> {
            if (result.succeeded()) {
                log.logp(INFO, getClass().getName(), "test_vertx_default_options.success",
                        JsonUtils.toVertxJsonObject(ProtobufUtils.protobuMessageToJson(result.result().body())).encodePrettily()
                );
                future.complete(result.result().body());
            } else {
                log.logp(SEVERE, getClass().getName(), "test_vertx_default_options.failure", "get-verticle-deployments failed", result.cause());
                future.completeExceptionally(result.cause());
            }
        };
    }

}