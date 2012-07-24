package no.kantega.publishing.admin.content.action;

import no.kantega.commons.client.util.RequestParameters;
import no.kantega.commons.client.util.ValidationErrors;
import no.kantega.commons.util.LocaleLabels;
import no.kantega.publishing.admin.AdminSessionAttributes;
import no.kantega.publishing.admin.content.util.SaveContentHelper;
import no.kantega.publishing.admin.viewcontroller.AdminController;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.common.data.Content;
import no.kantega.publishing.common.data.enums.AttributeDataType;
import no.kantega.publishing.common.exception.ContentNotFoundException;
import no.kantega.publishing.common.exception.MultipleEditorInstancesException;
import no.kantega.publishing.common.service.ContentManagementService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

public class AutoSaveContentAction extends AdminController {
    private View view;


    @Override
    public ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {


        HttpSession session = request.getSession();
        Content content = (Content)session.getAttribute(AdminSessionAttributes.CURRENT_EDIT_CONTENT);
        if (content == null) {
            throw new ContentNotFoundException(this.getClass().getName(), "");
        }

        RequestParameters param = new RequestParameters(request, "utf-8");
        // Id of page being edited, checked towards session to prevent problems with multiple edits at the same time
        int currentId = param.getInt("currentId");

        if (currentId != content.getId()) {
            throw new MultipleEditorInstancesException();
        }

        SaveContentHelper helper = new SaveContentHelper(request, content, AttributeDataType.CONTENT_DATA);

        ValidationErrors errors = new ValidationErrors();
        errors = helper.getHttpParameters(errors);

        ContentManagementService cms = new ContentManagementService(request);

        content.setMinorChange(true);

        if (content.getTitle().length() == 0) {
            content.setTitle(LocaleLabels.getLabel("", Aksess.getDefaultAdminLocale()));
        }

        Content savedContent = cms.autoSaveContent(content);

        Map<String, Object> model = new HashMap<String, Object>();


        if ((request.getHeader("Accept") != null) && request.getHeader("Accept").contains("application/json")) {
            response.setContentType("application/json");
        } else {
            // Needed for Firefox
            response.setContentType("text/plain");
        }

        model.put("contentId", savedContent.getId());

        return new ModelAndView(view);
    }

    public void setView(View view) {
        this.view = view;
    }
}
