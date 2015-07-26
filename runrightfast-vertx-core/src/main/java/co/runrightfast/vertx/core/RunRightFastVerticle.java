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
package co.runrightfast.vertx.core;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.concurrent.atomic.AtomicInteger;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;

/**
 * Base class for verticles, which provides support for :
 * <ol>
 * <li>logging
 * <li>config
 * <li>metrics
 * <li>healthchecks
 * </ol>
 *
 * Each verticle has its own scoped MetricRegistry and HealthCheckRegistry using the verticle's deployment id. Thus, if there are multiple instances of a
 * verticle running, they will share the same deployment id.
 *
 * Subclasses must override the {@link #startUp()} and {@link #shutDown()} methods, which are invoked by the corresponding {@link #start()} and {@link #stop()}
 * methods.
 *
 *
 * @author alfio
 */
public abstract class RunRightFastVerticle extends AbstractVerticle {

    private static final String LIFECYCLE_LOG_MSG = "%s verticle instance %d";
    private static final AtomicInteger instanceSequence = new AtomicInteger(0);

    protected final String CLASS_NAME = getClass().getName();
    protected final Logger log = Logger.getLogger(CLASS_NAME);

    protected MetricRegistry metricRegistry;
    protected HealthCheckRegistry healthCheckRegistry;

    protected int instanceId;

    /**
     * Performs the following:
     *
     * <ol>
     * <li>initializes the metric registry
     * <li>initializes the healthcheck regsistry
     * </ol>
     *
     * @param vertx Vertx
     * @param context Context
     */
    @Override
    public final void init(final Vertx vertx, final Context context) {
        super.init(vertx, context);
        this.metricRegistry = SharedMetricRegistries.getOrCreate(context.deploymentID());
        this.healthCheckRegistry = SharedHealthCheckRegistries.getOrCreate(context.deploymentID());
        this.instanceId = instanceSequence.incrementAndGet();
        log.logp(INFO, CLASS_NAME, "init", () -> String.format(LIFECYCLE_LOG_MSG, "initialized", instanceId));
    }

    @Override
    public final void start() throws Exception {
        log.logp(INFO, CLASS_NAME, "start", () -> String.format(LIFECYCLE_LOG_MSG, "starting", instanceId));
        try {
            metricRegistry.counter(RunRightFastVerticleMetrics.Counters.INSTANCE_STARTED.metricName).inc();
            startUp();
        } finally {
            log.logp(INFO, CLASS_NAME, "start", () -> String.format(LIFECYCLE_LOG_MSG, "started", instanceId));
        }
    }

    @Override
    public final void stop() throws Exception {
        log.logp(INFO, CLASS_NAME, "stop", () -> String.format(LIFECYCLE_LOG_MSG, "stopping", instanceId));
        try {
            shutDown();
        } finally {
            metricRegistry.counter(RunRightFastVerticleMetrics.Counters.INSTANCE_STARTED.metricName).dec();
            log.logp(INFO, CLASS_NAME, "stop", () -> String.format(LIFECYCLE_LOG_MSG, "stopped", instanceId));
        }
    }

    /**
     * Verticle specific start up
     */
    protected abstract void startUp();

    /**
     * Verticle specific start up shutdown
     */
    protected abstract void shutDown();

}
