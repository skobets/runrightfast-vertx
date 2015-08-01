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
package co.runrightfast.vertx.demo.verticles;

import co.runrightfast.vertx.core.RunRightFastVerticle;
import co.runrightfast.vertx.core.RunRightFastVerticleId;
import lombok.Getter;

/**
 *
 * @author alfio
 */
public final class TestVerticle extends RunRightFastVerticle {

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