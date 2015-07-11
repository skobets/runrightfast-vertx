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

import static co.runrightfast.vertx.core.VertxConstants.VERTX_METRIC_REGISTRY_NAME;
import co.runrightfast.vertx.core.VertxService;
import static co.runrightfast.vertx.core.VertxService.metricRegistry;
import co.runrightfast.vertx.core.utils.ConfigUtils;
import static co.runrightfast.vertx.core.utils.ConfigUtils.CONFIG_NAMESPACE;
import co.runrightfast.vertx.core.utils.JvmProcess;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import com.codahale.metrics.MetricFilter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
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

    private VertxService service;

    private List<VertxService> services = new LinkedList<>();

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

    /**
     * Test of getVertx method, of class VertxServiceImpl.
     */
    @Test
    public void test_vertx_default_options() {
        log.info("test_vertx_default_options");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-default")));
        ServiceUtils.start(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));
    }

    @Test
    public void test_vertx_metrics_options() {
        log.info("test_vertx_metrics_options");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-with-metrics")));
        ServiceUtils.start(service);
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
        assertThat(dropwizardMetricsOptions.getJmxDomain(), is("co.runrightfast"));
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
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-custom-non-clustered")));
        ServiceUtils.start(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(false));
        assertThat(vertx.isMetricsEnabled(), is(false));

        final VertxOptions vertxOptions = service.getVertxOptions();
        assertThat(vertxOptions.getBlockedThreadCheckInterval(), is(3000L));
        assertThat(vertxOptions.getClusterHost(), is(JvmProcess.getHost()));
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
    public void test_vertx_clustered() {
        log.info("test_vertx_clustered");
        service = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-clustered-1")));
        ServiceUtils.start(service);
        final Vertx vertx = service.getVertx();
        assertThat(vertx.isClustered(), is(true));

        final VertxOptions vertxOptions = service.getVertxOptions();
        assertThat(vertxOptions.getClusterManager(), is(notNullValue()));
        final ClusterManager clusterManager1 = vertxOptions.getClusterManager();
        log.log(INFO, "clusterManager1.getNodeID() = {0}", clusterManager1.getNodeID());

        final VertxService service2 = new VertxServiceImpl(config.getConfig(ConfigUtils.configPath(CONFIG_NAMESPACE, "vertx-clustered-2")));
        ServiceUtils.start(service2);
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

    }

}