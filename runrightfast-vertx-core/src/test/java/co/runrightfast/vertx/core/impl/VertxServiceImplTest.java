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
package co.runrightfast.vertx.core.impl;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.core.utils.ConfigUtils;
import static co.runrightfast.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import co.runrightfast.core.utils.JsonUtils;
import static co.runrightfast.core.utils.JvmProcess.HOST;
import co.runrightfast.core.utils.ProtobufUtils;
import co.runrightfast.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.VertxConstants.VERTX_METRIC_REGISTRY_NAME;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.application.ApplicationId;
import co.runrightfast.vertx.core.eventbus.EventBusAddress;
import co.runrightfast.vertx.core.eventbus.EventBusUtils;
import static co.runrightfast.vertx.core.eventbus.ProtobufMessageCodec.getProtobufMessageCodec;
import co.runrightfast.vertx.core.eventbus.ProtobufMessageProducer;
import static co.runrightfast.core.utils.VertxUtils.toJsonObject;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleManager;
import co.runrightfast.vertx.core.verticles.verticleManager.messages.GetVerticleDeployments;
import com.codahale.metrics.MetricFilter;
import com.google.common.collect.ImmutableSet;
import com.hazelcast.core.HazelcastInstance;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import javax.json.Json;
import javax.json.JsonObject;
import lombok.Getter;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author alfio
 */
@Log
public class VertxServiceImplTest {

    static class TestVerticle extends RunRightFastVerticle {

        @Getter
        private final RunRightFastVerticleId runRightFastVerticleId
                = RunRightFastVerticleId.builder()
                .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
                .name(getClass().getSimpleName())
                .version("1.0.0")
                .build();

        public TestVerticle(final AppEventLogger appEventLogger) {
            super(appEventLogger);
        }

        @Override
        protected void startUp() {
        }

        @Override
        protected void shutDown() {
        }

        @Override
        public Set<RunRightFastHealthCheck> getHealthChecks() {
            return ImmutableSet.of();
        }

    }

    static class TestVerticleParent extends RunRightFastVerticle {

        @Getter
        private final RunRightFastVerticleId runRightFastVerticleId
                = RunRightFastVerticleId.builder()
                .group(RunRightFastVerticleId.RUNRIGHTFAST_GROUP)
                .name(getClass().getSimpleName())
                .version("1.0.0")
                .build();

        public TestVerticleParent(final AppEventLogger appEventLogger) {
            super(appEventLogger);
        }

        @Override
        protected void startUp() {
            deployVerticles(ImmutableSet.of(
                    new RunRightFastVerticleDeployment(
                            () -> new TestVerticle(appEventLogger),
                            TestVerticle.class,
                            new DeploymentOptions())
            ));
        }

        @Override
        protected void shutDown() {
        }

        @Override
        public Set<RunRightFastHealthCheck> getHealthChecks() {
            return ImmutableSet.of();
        }

    }

    private final AppEventLogger appEventLogger = new AppEventJDKLogger(ApplicationId.builder()
            .group("co.runrightfast")
            .name("VertxServiceImplTest")
            .version("1.0.0").build()
    );

    private final Set<RunRightFastVerticleDeployment> deployments = ImmutableSet.of(
            new RunRightFastVerticleDeployment(
                    () -> new TestVerticle(appEventLogger),
                    TestVerticle.class,
                    new DeploymentOptions()
            ),
            new RunRightFastVerticleDeployment(
                    () -> new TestVerticleParent(appEventLogger),
                    TestVerticleParent.class,
                    new DeploymentOptions()
            )
    );

    private VertxService service;

    private final List<VertxService> services = new LinkedList<>();

    private static Config config;

    @BeforeClass
    public static void setUpClass() {
        config = ConfigUtils.loadConfig(String.format("%s.conf", VertxServiceImplTest.class.getSimpleName()), true);
    }

    @Before
    public void setUp() {
        ConfigFactory.invalidateCaches();
        metricRegistry.removeMatching(MetricFilter.ALL);
    }

    @After
    public void tearDown() {
        ServiceUtils.stop(service);
        services.stream().forEach(ServiceUtils::stop);
        services.clear();
    }

    @Test
    public void test_vertx_reference_default_options() {
        log.info("test_vertx_default_options");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx")), deployments, appEventLogger);
        ServiceUtils.start(service);
        services.add(service);
        log.info("service.getVertxOptions().getClusterHost() = " + service.getVertxOptions().getClusterHost());
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(true));
    }

    /**
     * Test of getVertx method, of class VertxServiceImpl.
     */
    @Test
    public void test_vertx_default_options() {
        log.info("test_vertx_default_options");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-default")), deployments, appEventLogger);
        ServiceUtils.start(service);
        services.add(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));
    }

    @Test
    public void test_vertx_metrics_options() {
        log.info("test_vertx_metrics_options");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-with-metrics")), deployments, appEventLogger);
        ServiceUtils.start(service);
        services.add(service);
        final Vertx vertx = service.getVertx();
        log.log(Level.INFO, "vertx.isClustered() = {0}", vertx.isClustered());
        log.log(Level.INFO, "vertx.isMetricsEnabled() = {0}", vertx.isMetricsEnabled());
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(true));

        final VertxOptions vertxOptions = service.getVertxOptions();
        final MetricsOptions metricsOptions = vertxOptions.getMetricsOptions();
        log.log(INFO, "metricsOptions class : {0}", metricsOptions.getClass().getName());
        final DropwizardMetricsOptions dropwizardMetricsOptions = (DropwizardMetricsOptions) metricsOptions;
        assertThat(dropwizardMetricsOptions.isJmxEnabled(), is(true));
        assertThat(dropwizardMetricsOptions.getJmxDomain(), is("co.runrightfast.vertx.metrics"));
        assertThat(dropwizardMetricsOptions.getRegistryName(), is(VERTX_METRIC_REGISTRY_NAME));
        assertThat(dropwizardMetricsOptions.getMonitoredEventBusHandlers().size(), is(2));
        assertThat(dropwizardMetricsOptions.getMonitoredHttpServerUris().size(), is(3));
        assertThat(dropwizardMetricsOptions.getMonitoredHttpClientUris().size(), is(4));

    }

    /**
     * Test of getVertx method, of class VertxServiceImpl.
     */
    @Test
    public void test_vertx_custom_options() {
        log.info("test_vertx_custom_options");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-custom-non-clustered")), deployments, appEventLogger);
        ServiceUtils.start(service);
        services.add(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));

        final VertxOptions vertxOptions = service.getVertxOptions();
        assertThat(vertxOptions.getBlockedThreadCheckInterval(), is(3000L));
        assertThat(vertxOptions.getClusterHost(), is(HOST));
        assertThat(vertxOptions.getHAGroup(), is("elasticsearch"));
        assertThat(vertxOptions.getClusterPingInterval(), is(1000L));
        assertThat(vertxOptions.getClusterPort(), is(1234));
        assertThat(vertxOptions.getClusterManager(), is(nullValue()));
        assertThat(vertxOptions.getEventLoopPoolSize(), is(20));
        assertThat(vertxOptions.getInternalBlockingPoolSize(), is(2000));
        assertThat(vertxOptions.getMaxEventLoopExecuteTime(), is(4000000000L));
        assertThat(vertxOptions.getMaxWorkerExecuteTime(), is(50000000000L));
        assertThat(vertxOptions.getQuorumSize(), is(3));
        assertThat(vertxOptions.getWarningExceptionTime(), is(3500000000L));
        assertThat(vertxOptions.getWorkerPoolSize(), is(30));
    }

    /**
     * Test of getVertx method, of class VertxServiceImpl.
     */
    @Test
    public void test_vertx_clustered() throws InterruptedException {
        log.info("test_vertx_clustered");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-clustered-1")), deployments, appEventLogger);
        ServiceUtils.start(service);
        services.add(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(true));

        final VertxOptions vertxOptions = service.getVertxOptions();
        assertThat(vertxOptions.getClusterManager(), is(notNullValue()));
        final ClusterManager clusterManager1 = vertxOptions.getClusterManager();
        log.log(INFO, "clusterManager1.getNodeID() = {0}", clusterManager1.getNodeID());

        final VertxService service2 = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-clustered-2")), deployments, appEventLogger);
        ServiceUtils.start(service2);
        services.add(service2);
        final Vertx vertx2 = service.getVertx();
        assertThat(vertx2.isClustered(), is(true));

        final VertxOptions vertxOptions2 = service.getVertxOptions();
        assertThat(vertxOptions2.getClusterManager(), is(notNullValue()));
        final ClusterManager clusterManager2 = vertxOptions2.getClusterManager();
        log.log(INFO, "clusterManager2.getNodeID() = {0}", clusterManager2.getNodeID());

        clusterManager2.getNodes().stream().forEach(node -> log.log(INFO, "clusterManager2 : node : {0}", node));
        clusterManager1.getNodes().stream().forEach(node -> log.log(INFO, "clusterManager1 : node : {0}", node));

        assertThat(clusterManager1.getNodes().size(), is(2));
        assertThat(clusterManager2.getNodes().size(), is(2));

        final HazelcastInstance hazelcast1 = service.getHazelcastInstance().get();
        final HazelcastInstance hazelcast2 = service2.getHazelcastInstance().get();

        final String counterId = UUID.randomUUID().toString();
        hazelcast1.getAtomicLong(counterId).set(1L);
        assertThat(hazelcast2.getAtomicLong(counterId).get(), is(1L));

        final ProtobufMessageProducer getVerticleDeploymentsMessageSender = new ProtobufMessageProducer(
                vertx.eventBus(),
                EventBusAddress.eventBusAddress(RunRightFastVerticleManager.VERTICLE_ID, "get-verticle-deployments"),
                getProtobufMessageCodec(GetVerticleDeployments.Request.getDefaultInstance()).get(),
                metricRegistry
        );

        final String GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS = "test_vertx_clustered/" + UUID.randomUUID().toString();

        final CountDownLatch latch = new CountDownLatch(2);
        vertx.eventBus().consumer(GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS, (final Message<GetVerticleDeployments.Response> responseMessage) -> {
            try {
                final GetVerticleDeployments.Response response = responseMessage.body();
                final JsonObject json = Json.createObjectBuilder()
                        .add("headers", toJsonObject(responseMessage.headers()))
                        .add("body", ProtobufUtils.protobuMessageToJson(response))
                        .build();
                log.info(JsonUtils.toVertxJsonObject(json).encodePrettily());
            } finally {
                latch.countDown();
            }
        });

        getVerticleDeploymentsMessageSender.publish(
                GetVerticleDeployments.Request.newBuilder().build(),
                EventBusUtils.withReplyToAddress(EventBusUtils.deliveryOptions(), GET_VERTICLE_DEPLOYMENTS_REPLY_TO_ADDRESS)
        );

        latch.await();

    }

}
