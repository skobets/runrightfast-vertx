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
package co.runrightfast.vertx.orientdb.verticle;

import co.runrightfast.core.application.event.AppEventLogger;
import static co.runrightfast.core.application.services.healthchecks.HealthCheckConfig.FailureSeverity.FATAL;
import co.runrightfast.core.application.services.healthchecks.RunRightFastHealthCheck;
import co.runrightfast.core.crypto.EncryptionService;
import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import static co.runrightfast.vertx.core.RunRightFastVerticleId.RUNRIGHTFAST_GROUP;
import co.runrightfast.vertx.core.utils.ServiceUtils;
import co.runrightfast.vertx.core.verticles.verticleManager.RunRightFastVerticleDeployment;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxHealthCheck;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxHealthCheck.ODatabaseDocumentTxHealthCheckBuilder;
import co.runrightfast.vertx.orientdb.ODatabaseDocumentTxSupplier;
import co.runrightfast.vertx.orientdb.OrientDBService;
import co.runrightfast.vertx.orientdb.classes.DocumentObject;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBService;
import co.runrightfast.vertx.orientdb.impl.EmbeddedOrientDBServiceConfig;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.Json;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Manages an embedded OrientDB server.
 *
 * Only a single instance of this verticle should be created per JVM process.
 *
 * Verticles that require use of the OrientDBService must be created by this verticle, which will provide them with the {@link OrientDBService} instance.
 *
 * @author alfio
 */
public final class OrientDBVerticle extends RunRightFastVerticle {

    public static final RunRightFastVerticleId VERTICLE_ID = RunRightFastVerticleId.builder()
            .group(RUNRIGHTFAST_GROUP)
            .name("orientdb")
            .version("0.1.0")
            .build();

    @Getter
    private final RunRightFastVerticleId runRightFastVerticleId = VERTICLE_ID;

    private final EmbeddedOrientDBServiceConfig config;

    private EmbeddedOrientDBService service;

    private final ImmutableSetMultimap<String, Class<? extends DocumentObject>> databaseClassesForHealthCheck;

    private final OrientDBRepositoryVerticleDeployment[] orientDBRepositoryVerticleDeployments;

    public OrientDBVerticle(
            final AppEventLogger appEventLogger,
            final EncryptionService encryptionService,
            @NonNull final EmbeddedOrientDBServiceConfig config,
            @NonNull SetMultimap<String, Class<? extends DocumentObject>> databaseClassesForHealthCheck,
            final OrientDBRepositoryVerticleDeployment... orientDBRepositoryVerticleDeployments) {
        super(appEventLogger, encryptionService);
        config.validate();

        this.config = config;
        this.databaseClassesForHealthCheck = ImmutableSetMultimap.copyOf(databaseClassesForHealthCheck);

        if (ArrayUtils.isNotEmpty(orientDBRepositoryVerticleDeployments)) {
            this.orientDBRepositoryVerticleDeployments = Arrays.copyOf(orientDBRepositoryVerticleDeployments, orientDBRepositoryVerticleDeployments.length);
        } else {
            this.orientDBRepositoryVerticleDeployments = new OrientDBRepositoryVerticleDeployment[0];
        }

    }

    @Override
    protected void startUp() {
        service = new EmbeddedOrientDBService(config);
        ServiceUtils.start(service);

        deployOrientDBRepositoryVerticles();
    }

    @Override
    protected void shutDown() {
        ServiceUtils.stop(service);
        service = null;
    }

    @Override
    public Set<RunRightFastHealthCheck> getHealthChecks() {
        return oDatabaseDocumentTxHealthChecks();
    }

    private Set<RunRightFastHealthCheck> oDatabaseDocumentTxHealthChecks() {
        ServiceUtils.awaitRunning(service);
        return service.getDatabaseNames().stream()
                .map(name -> {
                    final ODatabaseDocumentTxSupplier oDatabaseDocumentTxSupplier = service.getODatabaseDocumentTxSupplier(name).get();
                    final ODatabaseDocumentTxHealthCheckBuilder healthcheckBuilder = ODatabaseDocumentTxHealthCheck.builder().oDatabaseDocumentTxSupplier(oDatabaseDocumentTxSupplier);
                    final Set<Class<? extends DocumentObject>> classes = databaseClassesForHealthCheck.get(name);
                    if (CollectionUtils.isNotEmpty(classes)) {
                        classes.stream().forEach(healthcheckBuilder::documentObject);
                    } else {
                        warning.log("oDatabaseDocumentTxHealthChecks", () -> {
                            return Json.createObjectBuilder()
                            .add("database", name)
                            .add("message", "No OrientDB classes are configured for the healthcheck")
                            .build();
                        });
                    }
                    return healthcheckBuilder.build();
                }).map(healthcheck -> {
                    return RunRightFastHealthCheck.builder()
                    .config(healthCheckConfigBuilder()
                            .name("EmbeddedOrientDBServiceHealthCheck")
                            .severity(FATAL)
                            .build()
                    )
                    .healthCheck(healthcheck)
                    .build();
                }).collect(Collectors.toSet());
    }

    private void deployOrientDBRepositoryVerticles() {
        if (ArrayUtils.isEmpty(orientDBRepositoryVerticleDeployments)) {
            return;
        }

        final Set<RunRightFastVerticleDeployment> deployments = Arrays.stream(orientDBRepositoryVerticleDeployments).map(deployment -> {
            return new RunRightFastVerticleDeployment(
                    () -> {
                        final OrientDBRepositoryVerticle repo = deployment.getOrientDBRepositoryVerticle().get();
                        repo.setOrientDBService(service);
                        return repo;
                    },
                    deployment.getOrientDBRepositoryVerticleClass(),
                    deployment.getDeploymentOptions()
            );
        }).collect(Collectors.toSet());

        deployVerticles(deployments);
    }

}
