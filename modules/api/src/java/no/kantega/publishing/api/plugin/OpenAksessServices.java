package no.kantega.publishing.api.plugin;

import no.kantega.publishing.api.cache.SiteCache;
import no.kantega.publishing.api.configuration.SystemConfiguration;
import no.kantega.publishing.api.forms.service.FormService;
import no.kantega.publishing.api.plugin.config.PluginConfigProvider;
import no.kantega.publishing.api.templating.TemplateRenderer;
import no.kantega.publishing.api.ui.UIServices;
import no.kantega.security.api.identity.IdentityResolver;
import no.kantega.security.api.profile.ProfileManager;
import org.kantega.jexmec.PluginManager;

import javax.sql.DataSource;

/**
 * Services exposed to plugins by Flyt CMS
 */
public interface OpenAksessServices {

    public SiteCache getSiteCache();

    public DataSource getDataSource(DataSourceName name);

    public IdentityResolver getIdentityResolver(IdentityResolverName name);

    public ProfileManager getProfileManager(ProfileManagerName name);

    public FormService getFormService();

    public SystemConfiguration getSystemConfiguration();

    public PluginConfigProvider getPluginConfigProvider();

    public PluginManager<OpenAksessPlugin> getPluginManager();

    public UIServices getUIServices();

    public TemplateRenderer getTemplateRenderer();

    enum DataSourceName {
        aksessDataSource
    }

    enum IdentityResolverName {
        aksessIdentityResolver
    }

    enum ProfileManagerName {
        aksessProfileManager
    }

}
