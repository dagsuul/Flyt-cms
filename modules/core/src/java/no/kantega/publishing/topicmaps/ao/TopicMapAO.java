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

package no.kantega.publishing.topicmaps.ao;

import no.kantega.publishing.spring.RootContext;
import no.kantega.publishing.topicmaps.data.TopicMap;
import no.kantega.publishing.common.exception.ObjectInUseException;
import no.kantega.commons.exception.SystemException;

import java.util.List;

@Deprecated
public class TopicMapAO {
    private static final String AKSESS_TOPIC_MAP_DAO = "aksessTopicMapDao";

    public static List<TopicMap> getTopicMaps() throws SystemException {
        TopicMapDao dao = (TopicMapDao)RootContext.getInstance().getBean(AKSESS_TOPIC_MAP_DAO);
        return dao.getTopicMaps();
    }

    public static TopicMap getTopicMap(int id) throws SystemException {
        TopicMapDao dao = (TopicMapDao)RootContext.getInstance().getBean(AKSESS_TOPIC_MAP_DAO);
        return dao.getTopicMapById(id);
    }

    public static TopicMap getTopicMapByName(String name) throws SystemException {
        TopicMapDao dao = (TopicMapDao)RootContext.getInstance().getBean(AKSESS_TOPIC_MAP_DAO);
        return dao.getTopicMapByName(name);
    }

    public static TopicMap setTopicMap(TopicMap topicMap) throws SystemException {
        TopicMapDao dao = (TopicMapDao)RootContext.getInstance().getBean(AKSESS_TOPIC_MAP_DAO);
        return dao.saveOrUpdateTopicMap(topicMap);
    }

    public static void deleteTopicMap(int id) throws SystemException, ObjectInUseException {
        TopicMapDao dao = (TopicMapDao)RootContext.getInstance().getBean(AKSESS_TOPIC_MAP_DAO);
        dao.deleteTopicMap(id);
    }
}
