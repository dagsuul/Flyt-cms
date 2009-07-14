/*
 * Copyright 2009 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.kantega.publishing.admin.content.action;

import no.kantega.publishing.admin.viewcontroller.AdminController;
import no.kantega.publishing.common.Aksess;
import no.kantega.commons.client.util.RequestParameters;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class InsertLinkAction extends AdminController {
    private final String LINKTYPE_ATTACHMENT = "attachment";
    private final String LINKTYPE_INTERNAL = "internal";
    private final String LINKTYPE_EXTERNAL = "external";
    private final String LINKTYPE_EMAIL = "email";
    private final String LINKTYPE_ANCHOR = "anchor";
    private final String LINKTYPE_MEDIA = "multimedia";

    private String view;

    public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String linkType = request.getParameter("linkType");
        if (!LINKTYPE_ATTACHMENT.equals(linkType) &&
            !LINKTYPE_ANCHOR.equals(linkType) &&
            !LINKTYPE_INTERNAL.equals(linkType) &&
            !LINKTYPE_EXTERNAL.equals(linkType) &&
            !LINKTYPE_EMAIL.equals(linkType) &&
            !LINKTYPE_MEDIA.equals(linkType)) {
            linkType = LINKTYPE_EXTERNAL;
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("linkType", linkType);

        RequestParameters param = new RequestParameters(request);
        String url = param.getString("url");
        String anchor = param.getString("anchor");

        if (url != null) {
            if (url.indexOf("@") != -1) {
                linkType = LINKTYPE_EMAIL;
            }
        }

        if (url == null || url.length() == 0) {
            if (linkType.equals("external")) {
                url = "http://";
            }
        }
        if (anchor == null) {
            anchor = "";
        }

        model.put("url", url);
        model.put("anchor", anchor);

        boolean openInNewWindow = param.getBoolean("openInNewWindow", false);
        if (openInNewWindow || Aksess.doOpenLinksInNewWindow()) {
            model.put("openInNewWindow", Boolean.TRUE);
        }

        model.put(linkType + "Selected", "selected");

        if (Aksess.isSmartLinksDefaultChecked()) {
            model.put("smartLink", Boolean.TRUE);
        }

        return new ModelAndView(view, model);
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }
}
