/*
 * Copyright 2009 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.kantega.publishing.common.data.attributes;

import no.kantega.publishing.admin.content.behaviours.attributes.ImageAttributeValueXMLExporter;
import no.kantega.publishing.admin.content.behaviours.attributes.XMLAttributeValueExporter;

/**
 *
 */
public class ImageAttribute extends MediaAttribute {
    protected String filter = "image";

    public XMLAttributeValueExporter getXMLAttributeValueExporter() {
        return new ImageAttributeValueXMLExporter();
    }
}
