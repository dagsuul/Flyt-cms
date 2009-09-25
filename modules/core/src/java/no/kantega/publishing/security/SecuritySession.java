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

package no.kantega.publishing.security;

import no.kantega.commons.exception.ConfigurationException;
import no.kantega.commons.exception.SystemException;
import no.kantega.commons.log.Log;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.common.cache.SiteCache;
import no.kantega.publishing.common.data.BaseObject;
import no.kantega.publishing.common.data.Content;
import no.kantega.publishing.common.data.Site;
import no.kantega.publishing.org.OrganizationManager;
import no.kantega.publishing.security.data.CachedBaseObject;
import no.kantega.publishing.security.data.Role;
import no.kantega.publishing.security.data.User;
import no.kantega.publishing.security.login.PostLoginHandler;
import no.kantega.publishing.security.login.PostLoginHandlerFactory;
import no.kantega.publishing.security.realm.SecurityRealm;
import no.kantega.publishing.security.realm.SecurityRealmFactory;
import no.kantega.publishing.security.service.SecurityService;
import no.kantega.publishing.security.util.SecurityHelper;
import no.kantega.publishing.spring.RootContext;
import no.kantega.publishing.topicmaps.ao.TopicAO;
import no.kantega.publishing.topicmaps.data.Topic;
import no.kantega.security.api.identity.*;
import no.kantega.security.api.profile.Profile;
import no.kantega.security.api.profile.ProfileManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class SecuritySession {
    private static String SOURCE = "SecuritySession";

    private User user = null;
    private Identity identity = null;
    private SecurityRealm realm = null;

    // Husker sist tilgangssjekk
    private CachedBaseObject prevObject = null;
    private int prevPrivilege = -1;
    private boolean prevResult = false;


    /**
     * Henter en sikkerhetssesjon - Sjekker om en bruker er innlogget, setter i s�fall User og Identity objektet
     * @param request
     * @return
     * @throws SystemException
     */
    public static SecuritySession getInstance(HttpServletRequest request) throws SystemException {

        SecuritySession session = (SecuritySession) request.getSession(true).getAttribute("aksess.securitySession");
        if (session == null) {
            session = createNewInstance();
            request.getSession(true).setAttribute("aksess.securitySession", session);
        }

        IdentityResolver resolver = session.realm.getIdentityResolver();
        Identity identity = null;
        try {
            identity = resolver.getIdentity(request);
        } catch (IdentificationFailedException e) {
            throw new SystemException(SOURCE, "IdentificationFailedException", e);
        }
        Identity currentIdentity = session.identity;

        // Dersom bruker n� er p�logget eller innlogging er endret m� vi hente profil p� nytt
        if (identity != null && (currentIdentity == null || !identity.getUserId().equals(currentIdentity.getUserId()) || !identity.getDomain().equals(currentIdentity.getDomain()))) {
            // Opprett ny sesjon
            session = createNewInstance();
            request.getSession(true).setAttribute("aksess.securitySession", session);

            ProfileManager manager = session.realm.getProfileManager();
            Profile p = null;
            try {
                p = manager.getProfileForUser(identity);
            } catch (no.kantega.security.api.common.SystemException e) {
                throw new SystemException(SOURCE, "Feil ved henting av profil", e);
            }

            User user = new User();
            if (p != null) {
                user = SecurityHelper.createAksessUser(p);
            } else {
                user.setId(identity.getDomain() + ":" + identity.getUserId());
                user.setGivenName(identity.getUserId());
            }

            session.user = user;
            session.identity = identity;

            // Reset cachet verdi, bruker har logget inn, andre privilegier
            session.prevObject = null;

            // Innloggede brukere har lengre sesjonstimeout
            if (request.getSession().getMaxInactiveInterval() < Aksess.getSecuritySessionTimeout()) {
                request.getSession().setMaxInactiveInterval(Aksess.getSecuritySessionTimeout());
            }

            try {
                session.handlePostLogin(request);
            } catch (ConfigurationException e) {
                throw new SystemException(SOURCE, "Konfigurasjonsfeil", e);
            }
        } else if (identity == null && currentIdentity != null) {
            // Bruker er utlogget via ekstern tjeneste - lag blank sesjon           
            session = createNewInstance();
            request.getSession(true).setAttribute("aksess.securitySession", session);
            request.getSession(true).removeAttribute("adminMode");
        }

        return session;
    }

    public static SecuritySession createNewAdminInstance() throws SystemException {
        SecuritySession session = createNewInstance();

        User admin = new User();
        admin.setGivenName("Aksess");
        admin.setSurname("CMS");
        admin.setId("admin");

        Role role = new Role();
        role.setId(Aksess.getAdminRole());
        role.setName(Aksess.getAdminRole());
        admin.addRole(role);

        session.user = admin;

        return session;

    }

    private static SecuritySession createNewInstance() throws SystemException {
        SecuritySession session = new SecuritySession();
        session.realm = SecurityRealmFactory.getInstance();
        return session;
    }

    /**
     * H�ndterer n�r en ny bruker har blitt logget inn, henter roller, emner, orgenheter osv
     * @param request
     * @throws SystemException
     * @throws ConfigurationException
     */
    public void handlePostLogin(HttpServletRequest request) throws SystemException, ConfigurationException {

        List roles = realm.lookupRolesForUser(user.getId());
        for (int i = 0; i < roles.size(); i++) {
            Role role =  (Role)roles.get(i);
            user.addRole(role);
        }

        if (Aksess.isTopicMapsEnabled()) {
            // Hent topics for bruker
            List tmp = TopicAO.getTopicsBySID(user);
            for (int i = 0; i < tmp.size(); i++) {
                user.addTopic((Topic)tmp.get(i));
            }

            // Og for roller brukeren har tilgang til
            if (user.getRoles() != null) {
                for (int i = 0; i < roles.size(); i++) {
                    Role role =  (Role)roles.get(i);
                    tmp = TopicAO.getTopicsBySID(role);
                    for (int j = 0; j < tmp.size(); j++) {
                        user.addTopic((Topic)tmp.get(j));
                    }
                }
            }
        }

        Map orgManagers = RootContext.getInstance().getBeansOfType(OrganizationManager.class);
        if(orgManagers.size() > 0) {
            OrganizationManager manager = (OrganizationManager) orgManagers.values().iterator().next();
            List orgunits = manager.getOrgUnitsAboveUser(user.getId());
            user.setOrgUnits(orgunits);
        }

        PostLoginHandler plh = PostLoginHandlerFactory.newInstance();
        if (plh != null) {
            plh.handlePostLogin(user, request);
        }
    }

    public boolean isLoggedIn() {
        if (getUser() != null) {
            return true;
        } else {
            return false;
        }
    }

    public void initiateLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirect = request.getRequestURL().toString();
        String originalUri = (String)request.getAttribute("javax.servlet.error.request_uri");
        if (originalUri != null) {
            // Call via 404
            redirect = originalUri;
        }

        String query = request.getQueryString();
        if (query != null && query.length() > 0) {
            redirect += "?" + query;
        }

        IdentityResolver resolver = realm.getIdentityResolver();

        DefaultLoginContext loginContext = new DefaultLoginContext();
        loginContext.setRequest(request);
        loginContext.setResponse(response);
        try {
            loginContext.setTargetUri(new URI(redirect));
        } catch (URISyntaxException e) {
            Log.error(SOURCE, e, null, null);
        }

        resolver.initateLogin(loginContext);
    }


    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String redirect = request.getParameter("redirect");
        if (redirect == null || redirect.length() == 0) {
            redirect = Aksess.getContextPath();
        }        

        IdentityResolver resolver = realm.getIdentityResolver();
        DefaultLogoutContext logoutContext = new DefaultLogoutContext();
        logoutContext.setRequest(request);
        logoutContext.setResponse(response);
        try {
            logoutContext.setTargetUri(new URI(redirect));
        } catch (URISyntaxException e) {
            Log.error(SOURCE, e, null, null);
        }

        try {
            resolver.initiateLogout(logoutContext);
        } catch (Exception e) {
            // Ikke alle IdentityResolvers h�ndterer utlogging, f.eks NTLM
        }

        // Nullstill
        user = null;
        identity = null;

        // Reset cachet verdi, bruker har logget ut, andre privilegier
        prevObject = null;
    }

    public User getUser() {
        return user;
    }

    public Identity getIdentity() {
        return identity;
    }

    public boolean isUserInRole(String[] roles) {
        for (int i = 0; i < roles.length; i++) {
            String role = roles[i].trim();
            if (SecurityService.isUserInRole(user, role)) {
                return true;
            }
        }

        return false;
    }

    public boolean isUserInRole(String role) {
        return SecurityService.isUserInRole(user, role);
    }

    public boolean isAuthorized(BaseObject object, int privilege) throws SystemException {
        if (object instanceof Content) {
            // Sjekker om det ikke skal vises sider fra dette nettstedet
            Content c = (Content)object;
            Site site = SiteCache.getSiteById(c.getAssociation().getSiteId());
            if (site.isDisabled()) {
                return false;
            }
        }

        if (prevObject != null && prevObject.isSameAs(object) && privilege == prevPrivilege) {
            return prevResult;
        }

        prevResult = SecurityService.isAuthorized(user, object, privilege);
        prevObject = new CachedBaseObject(object);
        prevPrivilege = privilege;

        return prevResult;
    }

    public boolean isApprover(Content c) throws SystemException {
        return SecurityService.isApprover(user, c);
    }

    public List getAllRoles() throws SystemException {
        return realm.getAllRoles();
    }

    /**
     * Henter userId for alle brukere som har den gitte rollen.
     * @param role roleId
     * @return List med userIds (String)
     * @throws SystemException
     */
    public List getUsersWithRole(String role) throws SystemException {
        return realm.lookupUsersWithRole(role); 
    }

    public List searchUsers(String name) throws SystemException {
        return realm.searchUsers(name);
    }


    public SecurityRealm getRealm() {
        return realm;
    }
}
