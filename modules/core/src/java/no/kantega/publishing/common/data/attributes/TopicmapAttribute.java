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

import no.kantega.publishing.common.data.enums.AttributeProperty;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.topicmaps.data.Topic;
import no.kantega.publishing.topicmaps.data.TopicMap;
import no.kantega.publishing.topicmaps.ao.TopicAO;
import no.kantega.publishing.topicmaps.ao.TopicMapAO;
import no.kantega.commons.exception.SystemException;

import java.text.ParseException;
import java.util.List;

public class TopicmapAttribute  extends Attribute {

    public String getRenderer() {
        return "topicmap";
    }

    public List getTopicMaps() throws SystemException {
        List topicmaps = TopicMapAO.getTopicMaps();
        return topicmaps;
    }
}

