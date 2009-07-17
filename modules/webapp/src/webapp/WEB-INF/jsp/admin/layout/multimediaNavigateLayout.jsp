<%@ taglib prefix="kantega" uri="http://www.kantega.no/aksess/tags/commons" %>
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
    <link rel="stylesheet" type="text/css" href="<%=Aksess.getContextPath()%>/admin/css/navigate.css">
    <link rel="stylesheet" type="text/css" href="<%=Aksess.getContextPath()%>/admin/css/multimedia.css">
    <script type="text/javascript" src="<%=Aksess.getContextPath()%>/admin/js/navigate.jjs"></script>
    <script type="text/javascript">
        var currentItemIdentifier = -1;

        $(document).ready(function(){
            debug("$(document).ready(): multimedia");
            bindMultimediaupdateEvents();
            triggerMultimediaupdateEvent();//Must be fired at startup in order to load the navigator
        });



        /**
         * Contains the binding of all elements that are listening to the multimediaupdate event.
         * New global listeners to this event should be added here.
         */
        function bindMultimediaupdateEvents() {
            //Enables the navigator to listen to contentupdate events. Called every time a contentupdate event is fired.
            $("#Navigator").bind("multimediaupdate", function(e, itemIdentifier){
                debug("bindMultimediaupdateEvents(): "+e.type +" event received");
                if (!suppressNavigatorUpdate) {
                    updateNavigator(itemIdentifier, true);
                } else {
                    suppressNavigatorUpdate = false;
                    debug("bindMultimediaupdateEvents(): navigationUpdate suppressed");
                }
                updateMainPane(itemIdentifier, suppressNavigatorUpdate);
            });
        }

        /**
         * Changes the content of the main pane
         *
         * @param itemIdentifier
         * @param suppressNavigatorUpdate true/false.
         */
        function updateMainPane(itemIdentifier, suppressNavigatorUpdate) {
            debug("updateMainPane(): itemIdentifier: " + itemIdentifier + ", suppressNavigatorUpdate: " + suppressNavigatorUpdate);
            if (suppressNavigatorUpdate) {
                suppressNavigatorUpdate = true;
            }
            $("#MultimediaFolders").load(getViewFolderAction(), {itemIdentifier: itemIdentifier}, function(success){
                addMediaitemClickListeners();
            });
        }

        /**
         * Adds click listeners to the different media types and decides actions on these clicks.
         */
        function addMediaitemClickListeners() {
            $("#MultimediaFolders .folder").click(function(){
                debug("addMediaitemClickListeners(): folder click recieved");
                var idAttr = $(this).attr("id");
                currentItemIdentifier = idAttr.substring("Media".length, idAttr.length);
                triggerMultimediaupdateEvent();
            });

            $("#MultimediaFolders .media").click(function(){
                debug("addMediaitemClickListeners(): media click recieved");
                var idAttr = $(this).attr("id");
                currentItemIdentifier = idAttr.substring("Media".length, idAttr.length);
                alert("N� skal bilderedigeringssiden �pnes");
                //window.location.href = 'xxxx';
            });
        }

        /**
         * Sets the context (right click) menus in the navigator.
         */
        function setContextMenus() {
            setContextMenu("multimedia", ['paste']);
            setContextMenu("folder", ['paste']);
        }

        function getNavigatorAction() {
            return "<%=Aksess.getContextPath()%>/admin/multimedia/MultimediaNavigator.action";
        }

        function getViewFolderAction() {
            return "<%=Aksess.getContextPath()%>/admin/multimedia/ViewFolder.action";
        }

        function getItemIdentifierFromNavigatorHref(href) {
            return getQueryParam("itemIdentifier", href);
        }

        function getCurrentItemIdentifier() {
            return currentItemIdentifier;
        }

        function onNavigatorTitleClick(elm) {
            var href = elm.attr("href");
            var itemIdentifier = getItemIdentifierFromNavigatorHref(href);

            currentItemIdentifier = itemIdentifier;
            triggerMultimediaupdateEvent();
        }
        function triggerMultimediaupdateEvent() {
            debug("triggerMultimediaupdateEvent(): mediaupdate event triggered");
            $.event.trigger("multimediaupdate",currentItemIdentifier);
        }

        function setLayoutSpecificSizes() {
            var paddingTop = $("#MultimediaFolders").css("padding-top");
            paddingTop = paddingTop.substring(0, paddingTop.indexOf("px"));
            var paddingBottom = $("#MultimediaFolders").css("padding-bottom");
            paddingBottom = paddingBottom.substring(0, paddingBottom.indexOf("px"));
            var multimediaFoldersHeight = $("#MainPane").height()-paddingTop-paddingBottom;

            $("#MultimediaFolders").css("height", multimediaFoldersHeight + "px");
        }

        function getNavigatorParams() {
            var params = new Object();
            return params;
        }
    </script>

</kantega:section>

<kantega:section id="topMenu">
    <%@include file="fragments/topMenu.jsp"%>
</kantega:section>

<kantega:section id="modesMenu">

</kantega:section>

<kantega:section id="toolsMenu">

</kantega:section>

<kantega:section id="body">
    <kantega:getsection id="content"/>
</kantega:section>


<%@include file="commonLayout.jsp"%>