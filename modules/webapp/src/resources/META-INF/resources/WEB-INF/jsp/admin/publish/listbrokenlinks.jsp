<%@ page import="no.kantega.publishing.common.Aksess" %>
<%@ page contentType="text/html;charset=utf-8" language="java" pageEncoding="utf-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="kantega" uri="http://www.kantega.no/aksess/tags/commons" %>
<%@ taglib prefix="admin" uri="http://www.kantega.no/aksess/tags/admin" %>
<%@ taglib prefix="aksess" uri="http://www.kantega.no/aksess/tags/aksess" %>

<%--
  ~ Copyright 2009 Kantega AS
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<kantega:section id="head">
    <script src="<kantega:expireurl url="/admin/js/linkcheck.js"/>"></script>
    <script>
        $(document).ready(function () {
            var btn = $('#thisPageBtn');
            btn.click(function (event) {
                openaksess.linkcheck.currentUrl = "${currentNavigateContent.url}";
                openaksess.linkcheck.updateLinkList("title", {thisPageOnly: true});
            });
        });
    </script>
</kantega:section>

<admin:box>
    <h1>
        <c:choose>
            <c:when test="${thisPageOnly}">
                <kantega:label key="aksess.linkcheck.titlesinglepage" pageTitle="${currentNavigateContent.title}"/>
            </c:when>
            <c:otherwise>
                <kantega:label key="aksess.linkcheck.title" pageTitle="${currentNavigateContent.title}"/>
            </c:otherwise>
        </c:choose>
    </h1>
    <c:if test='${lastChecked != ""}'>
        <p><kantega:label key="aksess.linkcheck.lastChecked"/>: ${lastChecked}</p>
    </c:if>
    <input id="thisPageBtn" type="button" class="ui-button" value="<kantega:label key="aksess.linkcheck.onlyThisPage"/>">
    <c:choose>
        <c:when test="${not empty brokenLinks}">
            <table class="fullWidth">
                <thead>
                <tr>
                    <th><a href="title"><kantega:label key="aksess.linkcheck.page"/></a></th>
                    <th><a href="title"><kantega:label key="aksess.linkcheck.field"/></a></th>
                    <th><a href="url"><kantega:label key="aksess.linkcheck.url"/></a></th>
                    <th><a href="status"><kantega:label key="aksess.linkcheck.status"/></a></th>
                    <th><a href="lastchecked"><kantega:label key="aksess.linkcheck.lastchecked"/></a></th>
                    <th><a href="timeschecked"><kantega:label key="aksess.linkcheck.timeschecked"/></a></th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="link" items="${brokenLinks}" varStatus="status">
                    <tr class="tableRow${status.index mod 2}" valign="top">
                        <td>
                            <a href="${pageContext.request.contextPath}/admin/publish/Navigate.action?contentId=${link.contentId}" target="_top">${link.contentTitle}</a>
                        </td>
                        <td>
                            <c:if test="${link.attributeName != null}">
                                ${link.attributeName}
                            </c:if>
                        </td>
                        <c:set var="url" value="${link.url}"/>
                        <%
                            String url = (String) pageContext.getAttribute("url");
                            if (url.startsWith(Aksess.VAR_WEB)) {
                                url = Aksess.getContextPath() + url.substring(Aksess.VAR_WEB.length());
                            }
                        %>
                        <td><a target="external" href="<%=url%>">

                            <%= url.length() > 40 ? url.substring(0, 40) + "..." : url%>
                        </a></td>
                        <td>
                            <c:choose>
                                <c:when test="${link.status == 'UNKNOWN_HOST'}"><kantega:label
                                        key="aksess.linkcheck.statuses.2"/></c:when>
                                <c:when test="${link.status == 'HTTP_NOT_200'}">
                                    <c:choose>
                                        <c:when test="${link.httpStatus == 401}"><kantega:label
                                                key="aksess.linkcheck.httpstatus.401"/></c:when>
                                        <c:when test="${link.httpStatus == 404}"><kantega:label
                                                key="aksess.linkcheck.httpstatus.404"/></c:when>
                                        <c:when test="${link.httpStatus == 500}"><kantega:label
                                                key="aksess.linkcheck.httpstatus.500"/></c:when>
                                        <c:otherwise>HTTP ${link.httpStatus}</c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:when test="${link.status == 'IO_EXCEPTION'}"><kantega:label
                                        key="aksess.linkcheck.statuses.4"/></c:when>
                                <c:when test="${link.status == 'CONNECTION_TIMEOUT'}"><kantega:label
                                        key="aksess.linkcheck.statuses.5"/></c:when>
                                <c:when test="${link.status == 'CIRCULAR_REDIRECT'}"><kantega:label
                                        key="aksess.linkcheck.statuses.6"/></c:when>
                                <c:when test="${link.status == 'CONNECT_EXCEPTION'}"><kantega:label
                                        key="aksess.linkcheck.statuses.7"/></c:when>
                                <c:when test="${link.status == 'CONTENT_AP_NOT_FOUND'}"><kantega:label
                                        key="aksess.linkcheck.statuses.8"/></c:when>
                                <c:when test="${link.status == 'INVALID_URL'}"><kantega:label
                                        key="aksess.linkcheck.statuses.9"/></c:when>
                                <c:when test="${link.status == 'ATTACHMENT_AP_NOT_FOUND'}"><kantega:label key="aksess.linkcheck.statuses.10"/></c:when>
                                <c:when test="${link.status == 'MULTIMEDIA_AP_NOT_FOUND'}"><kantega:label key="aksess.linkcheck.statuses.11"/></c:when>
                                <c:otherwise>${link.status}</c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${link.lastChecked != null}">
                                <admin:formatdate date="${link.lastChecked}"/>
                            </c:if>
                        </td>
                        <td>${link.timesChecked}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <div class="ui-state-highlight">
                <kantega:label key="aksess.linkcheck.help"/>
            </div>

        </c:when>
        <c:otherwise>
            <div class="ui-state-highlight">
                <c:choose>
                    <c:when test="${thisPageOnly}">
                        <kantega:label key="aksess.linkcheck.nobrokenlinkssinglepage"/>
                    </c:when>
                    <c:otherwise>
                        <kantega:label key="aksess.linkcheck.nobrokenlinks"/>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:otherwise>
    </c:choose>
</admin:box>
