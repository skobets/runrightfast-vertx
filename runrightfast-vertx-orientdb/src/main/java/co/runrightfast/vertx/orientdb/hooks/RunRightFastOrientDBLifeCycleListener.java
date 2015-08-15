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

import co.runrightfast.vertx.core.utils.LoggingUtils.JsonLog;
import com.orientechnologies.orient.core.db.ODatabaseInternal;
import com.orientechnologies.orient.core.db.ODatabaseLifecycleListener;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import java.util.Set;
import javax.json.Json;
import lombok.Builder;
import lombok.extern.java.Log;
import org.apache.commons.collections4.CollectionUtils;

/**
 *
 * @author alfio
 */
@Log
@Builder
public class RunRightFastOrientDBLifeCycleListener implements ODatabaseLifecycleListener {

    private final Set<ORecordHook> databaseHooks;

    private final JsonLog info = JsonLog.newInfoLog(log, getClass().getName());

    @Override
    public PRIORITY getPriority() {
        return PRIORITY.REGULAR;
    }

    @Override
    public void onCreate(final ODatabaseInternal odi) {
        info.log("onCreate", () -> Json.createObjectBuilder().add("db", odi.getName()).build());
    }

    @Override
    public void onOpen(final ODatabaseInternal odi) {
        if (CollectionUtils.isNotEmpty(databaseHooks)) {
            databaseHooks.stream().forEach(hook -> {
                odi.getUnderlying().registerHook(hook);
                info.log("onOpen", () -> Json.createObjectBuilder().add("db", odi.getName()).add("registerHook", hook.getClass().getName()).build());
            });
        }
        info.log("onOpen", () -> Json.createObjectBuilder().add("db", odi.getName()).build());
    }

    @Override
    public void onClose(final ODatabaseInternal odi) {
        info.log("onClose", () -> Json.createObjectBuilder().add("db", odi.getName()).build());
    }

    @Override
    public void onDrop(final ODatabaseInternal odi) {
        info.log("onDrop", () -> Json.createObjectBuilder().add("db", odi.getName()).build());
    }

    @Override
    public void onCreateClass(final ODatabaseInternal odi, final OClass oclass) {
        info.log("onCreateClass", () -> Json.createObjectBuilder().add("db", odi.getName()).add("class", oclass.getName()).build());
    }

    @Override
    public void onDropClass(final ODatabaseInternal odi, final OClass oclass) {
        info.log("onDropClass", () -> Json.createObjectBuilder().add("db", odi.getName()).add("class", oclass.getName()).build());
    }

}