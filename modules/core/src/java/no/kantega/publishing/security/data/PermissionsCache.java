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

package no.kantega.publishing.security.data;

import no.kantega.commons.exception.SystemException;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.common.data.BaseObject;
import no.kantega.publishing.security.ao.PermissionsAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionsCache {
    private static final Logger log = LoggerFactory.getLogger(PermissionsCache.class);

    private static final Map<String, List<Permission>> permissions = new ConcurrentHashMap<>();
    private static Date lastUpdate = null;

    public static List<Permission> getPermissions(BaseObject object) throws SystemException {
        if ((lastUpdate == null) || (Aksess.getDatabaseCacheTimeout() > 0 && lastUpdate.getTime() + (Aksess.getDatabaseCacheTimeout()) < new Date().getTime())) {
            reloadCache();
        }
        String key = String.format("%s/%s", object.getSecurityId() , object.getObjectType());
        return permissions.get(key);
    }

    public static void reloadCache() throws SystemException {
        log.debug( "Loading cache");

        synchronized (permissions) {
            lastUpdate  = new Date();

            Map<String, List<Permission>> tmp = PermissionsAO.getPermissionMap();
            permissions.clear();
            permissions.putAll(tmp);
        }
    }
}
