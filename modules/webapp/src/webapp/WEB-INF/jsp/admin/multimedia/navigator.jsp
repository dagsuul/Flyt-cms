<%--
~ Copyright 2009 Kantega AS
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~  http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
--%>
<%@ page contentType="text/html;charset=utf-8" language="java" pageEncoding="utf-8" %>
<%@ taglib prefix="admin" uri="http://www.kantega.no/aksess/tags/admin"%>
<script type="text/javascript">
    function allowDrop(ev){
        ev.preventDefault();
        ev.target.setAttribute("style", "background-color: #5CB6E1; color: #FFFFFF; font-weight: bold");
    }
    function drop(ev){
        ev.preventDefault();
        ev.target.appendChild(document.getElementById(data));
    }
    function dragLeave(ev){
        ev.preventDefault();
        ev.target.removeAttribute("style");
    }
</script>

<admin:printmultimedianavigator root="${mediaArchiveRoot}" currentId="${itemIdentifier}"/>

<div id="NavigatorState" style="display: none;">
    <div class="expand">true</div>
    <div class="getFoldersOnly">${getFoldersOnly}</div>
    <div class="openFolders">${openFolders}</div>
</div>