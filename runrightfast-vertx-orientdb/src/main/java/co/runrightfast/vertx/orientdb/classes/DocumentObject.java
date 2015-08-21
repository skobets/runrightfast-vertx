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
package co.runrightfast.vertx.orientdb.classes;

import com.orientechnologies.orient.core.record.impl.ODocument;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author alfio
 */
@RequiredArgsConstructor
public abstract class DocumentObject {

    @NonNull
    @Getter
    protected final ODocument document;

    public DocumentObject() {
        this.document = new ODocument(getClass().getSimpleName());
    }

    public void save() {
        this.document.save();
    }

    public void delete() {
        this.document.delete();
    }

    public String toJSON() {
        return document.toJSON();
    }

}