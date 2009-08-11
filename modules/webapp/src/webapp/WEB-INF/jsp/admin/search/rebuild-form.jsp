<%@ page contentType="text/html;charset=utf-8" language="java" pageEncoding="iso-8859-1" %>
<%@ include file="../../../../admin/include/jsp_header.jsf" %>
<%--
  ~ Copyright 2009 Kantega AS
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
	<title>rebuild-form.jsp</title>
    <link rel="stylesheet" type="text/css" href="../css/<%=skin%>.css">
</head>
<body class="bodyWithMargin">
<form action="RebuildIndex.action" method="POST">
    <input type="checkbox" checked="true" name="rebuild">Gjenoppbygg indeks<br>
    <input type="checkbox" checked="true" name="optimize">Optimaliser indeks<br>
    <input type="checkbox" checked="true" name="spelling">Oppdater ordliste for stavekontroll<br>
    <input type="submit" value="Start"/>
</form>
</body>
</html>
<%@ include file="../../../../admin/include/jsp_footer.jsf" %>