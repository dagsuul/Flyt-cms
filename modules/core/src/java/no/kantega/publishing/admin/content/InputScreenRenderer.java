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

package no.kantega.publishing.admin.content;

import no.kantega.commons.client.util.ValidationError;
import no.kantega.commons.client.util.ValidationErrors;
import no.kantega.commons.exception.InvalidFileException;
import no.kantega.commons.exception.SystemException;
import no.kantega.commons.log.Log;
import no.kantega.commons.util.LocaleLabels;
import no.kantega.publishing.admin.content.util.AttributeHelper;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.common.cache.ContentTemplateCache;
import no.kantega.publishing.common.cache.MetadataTemplateCache;
import no.kantega.publishing.common.cache.SiteCache;
import no.kantega.publishing.common.data.Content;
import no.kantega.publishing.common.data.ContentTemplate;
import no.kantega.publishing.common.data.Site;
import no.kantega.publishing.common.data.attributes.Attribute;
import no.kantega.publishing.common.data.attributes.HtmltextAttribute;
import no.kantega.publishing.common.data.enums.AttributeDataType;
import no.kantega.publishing.common.exception.InvalidTemplateException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputScreenRenderer {
    private static final String SOURCE = "aksess.admin.InputScreenRenderer";

    private PageContext pageContext = null;
    private Content content = null;
    private int attributeType = -1;

    public InputScreenRenderer(PageContext pageContext, Content content, int attributeType) throws SystemException, InvalidFileException, InvalidTemplateException {
        this.pageContext = pageContext;
        this.content  = content;
        this.attributeType = attributeType;
    }


    /**
     * Lager inputskjermbilde ved � g� gjennom alle attributter
     */
    public void generateInputScreen() throws IOException, SystemException, ServletException {
        JspWriter out = pageContext.getOut();
        ServletRequest request = pageContext.getRequest();

        Map<String, List<ValidationError>> fieldErrors = new HashMap<String, List<ValidationError>>();
        ValidationErrors errors = (ValidationErrors)request.getAttribute("errors");
        if (errors != null) {
            for (ValidationError error : errors.getErrors()) {
                if (error.getField() != null && error.getField().length() > 0) {
                    List<ValidationError> errorsForField = fieldErrors.get(error.getField());
                    if (errorsForField == null) {
                        errorsForField = new ArrayList<ValidationError>();
                        fieldErrors.put(error.getField(), errorsForField);
                    }
                    errorsForField.add(error);                    
                }
            }
        }

        ContentTemplate template = null;
        if (attributeType == AttributeDataType.CONTENT_DATA) {
            template = ContentTemplateCache.getTemplateById(content.getContentTemplateId(), true);
        } else if (attributeType == AttributeDataType.META_DATA && content.getMetaDataTemplateId() > 0) {
            template = MetadataTemplateCache.getTemplateById(content.getContentTemplateId(), true);
        }

        String globalHelpText = null;
        if (template != null) {
            globalHelpText = template.getHelptext();
        }

        if (globalHelpText != null && globalHelpText.length() > 0) {
            out.print("<div id=\"TemplateGlobalHelpText\" class=\"ui-state-highlight\">" + globalHelpText + "</div>");
        }

        int tabIndex = 100; // Angir tabindex for � f� cursor til � hoppe til rette felter
        List attrlist = content.getAttributes(attributeType);
        for (int i = 0; i < attrlist.size(); i++) {
            Attribute attr = (Attribute)attrlist.get(i);
            if (attr.isEditable() && !attr.isHidden(content)) {
                String value = attr.getValue();
                if (value == null || value.length() == 0) {
                    attr.setValue("");
                }

                // Skriver ut felt ved � inkludere JSP for hver attributt
                attr.setTabIndex(tabIndex);
                tabIndex += 10;

                request.setAttribute("content", content);
                request.setAttribute("attribute", attr);
                request.setAttribute("fieldName", AttributeHelper.getInputFieldName(attr.getName()));

                try {
                    if (fieldErrors.get(attr.getName()) != null) {
                        out.print("\n<div class=\"contentAttribute error\">\n");
                    } else {
                        out.print("\n<div class=\"contentAttribute\">\n");
                    }
                    pageContext.include("/admin/publish/attributes/" + attr.getRenderer() +".jsp");
                    out.print("\n");
                    String helptext = attr.getHelpText();
                    if (helptext != null && helptext.length() > 0) {
                        out.print("<div class=\"ui-state-highlight\">" + helptext + "</div>\n");
                    }
                    if (attr.inheritsFromAncestors()) {
                        String inheritText = LocaleLabels.getLabel("aksess.editcontent.inheritsfromancestors", Aksess.getDefaultAdminLocale());
                        out.print("<div class=\"ui-state-highlight\">" + inheritText + "</div>\n");
                    }
                    out.print("</div>\n");
                } catch (Exception e) {
                    out.print("</div>\n");
                    Log.error(SOURCE, e, null, null);
                    String errorMessage = LocaleLabels.getLabel("aksess.editcontent.exception", Aksess.getDefaultAdminLocale());
                    out.print("<div class=\"errorText\">" + errorMessage + ":" + attr.getTitle() + "</div>\n");
                }
            }
        }
    }
}
