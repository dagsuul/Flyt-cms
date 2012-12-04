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

package no.kantega.publishing.admin.content.action;

import no.kantega.commons.exception.NotAuthorizedException;
import no.kantega.publishing.common.service.ContentManagementService;
import no.kantega.publishing.common.service.lock.LockManager;
import no.kantega.publishing.common.data.ContentIdentifier;
import no.kantega.publishing.common.data.Content;
import no.kantega.publishing.common.data.Association;
import no.kantega.publishing.admin.AdminSessionAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

public class CancelEditAction implements Controller {

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        ContentManagementService aksessService = new ContentManagementService(request);

        Content content = (Content)session.getAttribute(AdminSessionAttributes.CURRENT_EDIT_CONTENT);
        if (content != null) {
            LockManager.releaseLock(content.getId());
            if (content.isNew()) {
                content = getParentContent(aksessService, content);
            } else {
                // Fetch latest version
                if (content.isAutoSaved() && content.shouldDeleteAutosavedVersionOnCancelEdit()) {
                    ContentIdentifier cid = content.getContentIdentifier();
                    if (isFirstVersion(content)) {
                        content = getParentContent(aksessService, content);
                        aksessService.deleteContent(cid);
                    } else {
                        cid.setVersion(content.getVersion());
                        aksessService.deleteContentVersion(cid);
                        content = getLastContentVersion(aksessService, content);
                    }
                } else {
                    content = getLastContentVersion(aksessService, content);
                }
            }

            session.setAttribute(AdminSessionAttributes.CURRENT_NAVIGATE_CONTENT, content);
            session.removeAttribute(AdminSessionAttributes.CURRENT_EDIT_CONTENT);
        }

        return new ModelAndView(new RedirectView("Navigate.action"));
    }

    private Content getLastContentVersion(ContentManagementService aksessService, Content content) throws NotAuthorizedException {
        ContentIdentifier cidLastVersion = new ContentIdentifier();
        cidLastVersion.setAssociationId(content.getAssociation().getId());
        content = aksessService.getContent(cidLastVersion);
        return content;
    }

    private boolean isFirstVersion(Content content) {
        return content.getVersion() == 1;
    }

    private Content getParentContent(ContentManagementService aksessService, Content content) throws NotAuthorizedException {
        ContentIdentifier cid = new ContentIdentifier();
        cid.setAssociationId(content.getAssociation().getParentAssociationId());
        return aksessService.getContent(cid);
    }
}
