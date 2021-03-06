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
package co.runrightfast.vertx.orientdb.hooks;

import co.runrightfast.core.application.event.AppEventLogger;
import co.runrightfast.core.application.event.impl.AppEventJDKLogger;
import co.runrightfast.vertx.core.application.ApplicationId;
import static co.runrightfast.core.utils.JvmProcess.HOST;
import static co.runrightfast.vertx.orientdb.OrientDBConstants.ROOT_USER;
import co.runrightfast.vertx.orientdb.classes.Timestamped;
import co.runrightfast.vertx.orientdb.lifecycle.RunRightFastOrientDBLifeCycleListener;
import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabaseFactory;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.graph.handler.OGraphServerHandler;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerEntryConfiguration;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkListenerConfiguration;
import com.orientechnologies.orient.server.config.OServerNetworkProtocolConfiguration;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.config.OServerUserConfiguration;
import com.orientechnologies.orient.server.handler.OServerSideScriptInterpreter;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary;
import java.io.File;
import static java.util.logging.Level.INFO;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import test.co.runrightfast.vertx.orientdb.classes.EventLogRecord;

/**
 *
 * @author alfio
 */
@Log
public class RunRightFastOrientDBLifeCycleListenerTest {

    static final String CLASS_NAME = RunRightFastOrientDBLifeCycleListenerTest.class.getSimpleName();

    private static OServer server;
    static final File orientdbHome = new File("build/temp/orientdb");

    @BeforeClass
    public static void setUpClass() throws Exception {
        orientdbHome.mkdirs();
        FileUtils.cleanDirectory(orientdbHome);
        FileUtils.deleteDirectory(orientdbHome);
        log.logp(INFO, CLASS_NAME, "setUpClass", String.format("orientdbHome.exists() = %s", orientdbHome.exists()));

        final File configDirSrc = new File("src/test/resources/orientdb/config");
        final File configDirTarget = new File(orientdbHome, "config");
        FileUtils.copyFileToDirectory(new File(configDirSrc, "default-distributed-db-config.json"), configDirTarget);
        FileUtils.copyFileToDirectory(new File(configDirSrc, "hazelcast.xml"), configDirTarget);

        server = createOServer();
        server.activate();

        final File dbDir = new File(orientdbHome, String.format("databases/%s", RunRightFastOrientDBLifeCycleListenerTest.class.getSimpleName()));
        final String dbUrl = "plocal:" + dbDir.getAbsolutePath();
        try (final ODatabase db = new ODatabaseFactory().createDatabase("document", dbUrl).create()) {
            log.logp(INFO, CLASS_NAME, "setUpClass", String.format("created db = %s", db.getName()));

            final OClass timestampedClass = db.getMetadata().getSchema().createAbstractClass(Timestamped.class.getSimpleName());
            timestampedClass.createProperty(Timestamped.Field.created_on.name(), OType.DATETIME);
            timestampedClass.createProperty(Timestamped.Field.updated_on.name(), OType.DATETIME);

            final OClass logRecordClass = db.getMetadata().getSchema().createClass(EventLogRecord.class.getSimpleName()).setSuperClasses(ImmutableList.of(timestampedClass));
            logRecordClass.createProperty(EventLogRecord.Field.event.name(), OType.STRING);
        }
    }

    private static OServer createOServer() throws Exception {
        registerLifeCycleListener();
        server = OServerMain.create(true);
        server.setServerRootDirectory(orientdbHome.getAbsolutePath());
        final OServerConfiguration config = new OServerConfiguration();

        config.handlers = ImmutableList.<OServerHandlerConfiguration>builder()
                .add(oGraphServerHandler())
                .add(oHazelcastPlugin())
                .add(oServerSideScriptInterpreter())
                .build();

        config.network = new OServerNetworkConfiguration();
        config.network.protocols = ImmutableList.<OServerNetworkProtocolConfiguration>builder()
                .add(new OServerNetworkProtocolConfiguration("binary", ONetworkProtocolBinary.class.getName()))
                .build();
        final OServerNetworkListenerConfiguration binaryListener = new OServerNetworkListenerConfiguration();
        binaryListener.ipAddress = "0.0.0.0";
        binaryListener.protocol = "binary";
        binaryListener.portRange = "2424-2430";
        binaryListener.socket = "default";
        config.network.listeners = ImmutableList.<OServerNetworkListenerConfiguration>builder()
                .add(binaryListener)
                .build();

        config.users = new OServerUserConfiguration[]{
            new OServerUserConfiguration(ROOT_USER, "root", "*")
        };

        config.properties = new OServerEntryConfiguration[]{
            new OServerEntryConfiguration("db.pool.min", "1"),
            new OServerEntryConfiguration("db.pool.max", "50")
        };

        server.startup(config);
        return server;
    }

    private static OServerHandlerConfiguration oGraphServerHandler() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OGraphServerHandler.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("graph.pool.max", "50")
        };
        return config;
    }

    private static OServerHandlerConfiguration oHazelcastPlugin() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OHazelcastPlugin.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("nodeName", HOST),
            new OServerParameterConfiguration("configuration.db.default", new File(orientdbHome, "config/default-distributed-db-config.json").getAbsolutePath()),
            new OServerParameterConfiguration("configuration.hazelcast", new File(orientdbHome, "config/hazelcast.xml").getAbsolutePath())
        };
        return config;
    }

    private static OServerHandlerConfiguration oServerSideScriptInterpreter() {
        final OServerHandlerConfiguration config = new OServerHandlerConfiguration();
        config.clazz = OServerSideScriptInterpreter.class.getName();
        config.parameters = new OServerParameterConfiguration[]{
            new OServerParameterConfiguration("enabled", "true"),
            new OServerParameterConfiguration("allowedLanguages", "SQL")
        };
        return config;
    }

    @AfterClass
    public static void tearDownClass() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testLifeCycleListener() throws Exception {
        try (final ODatabase db = server.openDatabase("document", getClass().getSimpleName(), ROOT_USER, "root")
                .registerHook(new SetCreatedOnAndUpdatedOn())
                .activateOnCurrentThread()) {
            testSavingEventLogRecord(db, "testLifeCycleListener");
        }

    }

    @Test
    public void testPooling() throws Exception {
        try (final ODatabase db = new OPartitionedDatabasePoolFactory().get("plocal:" + getClass().getSimpleName(), "writer", "writer").acquire()) {
            testSavingEventLogRecord(db, "testPooling");
        }
    }

    private void testSavingEventLogRecord(final ODatabase db, final String testName) throws Exception {
        db.registerHook(new SetCreatedOnAndUpdatedOn());

        final EventLogRecord eventLogRecord = new EventLogRecord();
        try {
            db.begin();
            eventLogRecord.setEvent("app.started");
            eventLogRecord.save();
            db.commit();
        } catch (final Exception e) {
            db.rollback();
            throw e;
        }

        log.logp(INFO, CLASS_NAME, testName, String.format("doc = %s", eventLogRecord.toJSON()));
        assertThat(eventLogRecord.getCreatedOn(), is(notNullValue()));
        assertThat(eventLogRecord.getUpdatedOn(), is(notNullValue()));
    }

    private static void registerLifeCycleListener() {
        final ApplicationId appId = ApplicationId.builder().group("co.runrightfast").name("runrightfast-vertx-orientdb").version("1.0.0").build();
        final AppEventLogger appEventLogger = new AppEventJDKLogger(appId);
        Orient.instance().addDbLifecycleListener(new RunRightFastOrientDBLifeCycleListener(appEventLogger));
    }

}
