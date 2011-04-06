package no.kantega.publishing.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import no.kantega.commons.exception.ConfigurationException;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.common.ao.AssociationAO;
import no.kantega.publishing.common.ao.EventLogAO;
import no.kantega.publishing.common.ao.XMLCacheAO;
import no.kantega.publishing.common.data.Association;
import no.kantega.publishing.common.data.EventLogEntry;
import no.kantega.publishing.common.data.XMLCacheEntry;
import no.kantega.publishing.common.data.enums.Event;
import no.kantega.publishing.common.data.enums.ObjectType;
import no.kantega.publishing.common.data.enums.ServerType;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 *
 */
public class SlaveCacheExpiratorJob {


    private Logger log = Logger.getLogger(getClass());

    private long lastRun = System.currentTimeMillis();

    private EventLogAO eventLogAO;

    private CacheManager cacheManager;

    public void execute() {
        if (Aksess.getServerType() != ServerType.SLAVE) {
            log.error("This job should not run on server type " +Aksess.getServerType());
            return;
        }
        try {
            if(!Aksess.getConfiguration().getBoolean("caching.enabled", false)) {
                return;
            }
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        final Cache contentCache = cacheManager.getCache(CacheManagerFactory.CacheNames.ContentCache.name());
        final Cache contentListCache = cacheManager.getCache(CacheManagerFactory.CacheNames.ContentListCache.name());
        final Cache siteMapCache = cacheManager.getCache(CacheManagerFactory.CacheNames.SiteMapCache.name());
        // Find content that was changed

        long thisRun = System.currentTimeMillis();

        final List<EventLogEntry> entiresSinceLast = eventLogAO.createQuery().setFrom(new Date(lastRun)).setSubjectType(ObjectType.CONTENT).list();


        // Remove any Content objects (keyed by Association)
        for (EventLogEntry eventLogEntry : entiresSinceLast) {

            // If this was a move, flush everything
            if(Event.MOVE_CONTENT.equals(eventLogEntry.getEventName())) {
                contentCache.removeAll();
                // Flush the content list (search) cache
                contentListCache.removeAll();
                // Flush the site map cache
                siteMapCache.removeAll();
            }

            for(Association a : AssociationAO.getAssociationsByContentId(eventLogEntry.getSubjectId())) {
                final Object key = a.getAssociationId();
                log.debug("Expiring association " + a.getAssociationId());
                contentCache.remove(key);
            }
        }

        // Flush content list and site map if anything changed. Paranoid, but effective.
        if(entiresSinceLast.size() > 0) {
            log.debug("Flushing content list and site map caches");
            // Flush the content list (search) cache
            contentListCache.removeAll();
            // Flush the site map cache
            siteMapCache.removeAll();
        }

        // Flush any XML caches changed since last time
        for(XMLCacheEntry e : XMLCacheAO.getSummary()) {
            if(e.getLastUpdated().getTime() > lastRun) {
                final Object key = e.getId();
                log.debug("Flushing XML cache " + key);
                cacheManager.getCache(CacheManagerFactory.CacheNames.XmlCache.name()).remove(key);
            }
        }


        lastRun = thisRun;
        
    }

    public void setEventLogAO(EventLogAO eventLogAO) {
        this.eventLogAO = eventLogAO;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}