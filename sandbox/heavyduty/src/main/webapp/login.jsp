<%@ taglib prefix='c' uri='http://java.sun.com/jstl/core_rt' %>
<%@ page import="org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter" %>
<%@ page import="org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter" %>
<%@ page import="org.springframework.security.core.AuthenticationException" %>

<html>
  <head>
    <title>CUSTOM SPRING SECURITY LOGIN</title>
  </head>

  <body onload="document.f.j_username.focus();">
    <h1>CUSTOM SPRING SECURITY LOGIN</h1>

    <form name="f" action="<c:url value='j_spring_security_check'/>" method="POST">
      <table>
        <tr><td>User:</td><td><input type='text' name='j_username' /></td></tr>
        <tr><td>Password:</td><td><input type='password' name='j_password'/></td></tr>
        <tr><td><input type="checkbox" name="_spring_security_remember_me"></td><td>Don't ask for my password for two weeks</td></tr>

        <tr><td colspan='2'><input name="submit" type="submit"></td></tr>
        <tr><td colspan='2'><input name="reset" type="reset"></td></tr>
      </table>
    </form>
  </body>
</html>
