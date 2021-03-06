/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.config.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Luke Taylor
 * @author Ben Alex
 * @author Rob Winch
 */
public class FormLoginBeanDefinitionParser {
    protected final Log logger = LogFactory.getLog(getClass());

    private static final String ATT_LOGIN_URL = "login-processing-url";

    static final String ATT_LOGIN_PAGE = "login-page";
    private static final String DEF_LOGIN_PAGE = DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL;

    private static final String ATT_FORM_LOGIN_TARGET_URL = "default-target-url";
    private static final String ATT_ALWAYS_USE_DEFAULT_TARGET_URL = "always-use-default-target";
    private static final String DEF_FORM_LOGIN_TARGET_URL = "/";
    private static final String ATT_USERNAME_PARAMETER = "username-parameter";
    private static final String ATT_PASSWORD_PARAMETER = "password-parameter";

    private static final String ATT_FORM_LOGIN_AUTHENTICATION_FAILURE_URL = "authentication-failure-url";
    private static final String DEF_FORM_LOGIN_AUTHENTICATION_FAILURE_URL =
        DefaultLoginPageGeneratingFilter.DEFAULT_LOGIN_PAGE_URL + "?" + DefaultLoginPageGeneratingFilter.ERROR_PARAMETER_NAME;

    private static final String ATT_SUCCESS_HANDLER_REF = "authentication-success-handler-ref";
    private static final String ATT_FAILURE_HANDLER_REF = "authentication-failure-handler-ref";

    private final String defaultLoginProcessingUrl;
    private final String filterClassName;
    private final BeanReference requestCache;
    private final BeanReference sessionStrategy;
    private final boolean allowSessionCreation;
    private final BeanReference portMapper;
    private final BeanReference portResolver;

    private RootBeanDefinition filterBean;
    private RootBeanDefinition entryPointBean;
    private String loginPage;
    private String loginProcessingUrl;

    FormLoginBeanDefinitionParser(String defaultLoginProcessingUrl, String filterClassName,
            BeanReference requestCache, BeanReference sessionStrategy, boolean allowSessionCreation, BeanReference portMapper, BeanReference portResolver) {
        this.defaultLoginProcessingUrl = defaultLoginProcessingUrl;
        this.filterClassName = filterClassName;
        this.requestCache = requestCache;
        this.sessionStrategy = sessionStrategy;
        this.allowSessionCreation = allowSessionCreation;
        this.portMapper = portMapper;
        this.portResolver = portResolver;
    }

    public BeanDefinition parse(Element elt, ParserContext pc) {
        String loginUrl = null;
        String defaultTargetUrl = null;
        String authenticationFailureUrl = null;
        String alwaysUseDefault = null;
        String successHandlerRef = null;
        String failureHandlerRef = null;
        // Only available with form-login
        String usernameParameter = null;
        String passwordParameter = null;
        String authDetailsSourceRef = null;

        Object source = null;

        if (elt != null) {
            source = pc.extractSource(elt);
            loginUrl = elt.getAttribute(ATT_LOGIN_URL);
            WebConfigUtils.validateHttpRedirect(loginUrl, pc, source);
            defaultTargetUrl = elt.getAttribute(ATT_FORM_LOGIN_TARGET_URL);
            WebConfigUtils.validateHttpRedirect(defaultTargetUrl, pc, source);
            authenticationFailureUrl = elt.getAttribute(ATT_FORM_LOGIN_AUTHENTICATION_FAILURE_URL);
            WebConfigUtils.validateHttpRedirect(authenticationFailureUrl, pc, source);
            alwaysUseDefault = elt.getAttribute(ATT_ALWAYS_USE_DEFAULT_TARGET_URL);
            loginPage = elt.getAttribute(ATT_LOGIN_PAGE);
            successHandlerRef = elt.getAttribute(ATT_SUCCESS_HANDLER_REF);
            failureHandlerRef = elt.getAttribute(ATT_FAILURE_HANDLER_REF);
            authDetailsSourceRef = elt.getAttribute(AuthenticationConfigBuilder.ATT_AUTH_DETAILS_SOURCE_REF);


            if (!StringUtils.hasText(loginPage)) {
                loginPage = null;
            }
            WebConfigUtils.validateHttpRedirect(loginPage, pc, source);
            usernameParameter = elt.getAttribute(ATT_USERNAME_PARAMETER);
            passwordParameter = elt.getAttribute(ATT_PASSWORD_PARAMETER);
        }

        filterBean = createFilterBean(loginUrl, defaultTargetUrl, alwaysUseDefault, loginPage, authenticationFailureUrl,
                successHandlerRef, failureHandlerRef, authDetailsSourceRef);

        if (StringUtils.hasText(usernameParameter)) {
            filterBean.getPropertyValues().addPropertyValue("usernameParameter", usernameParameter);
        }
        if (StringUtils.hasText(passwordParameter)) {
            filterBean.getPropertyValues().addPropertyValue("passwordParameter", passwordParameter);
        }

        filterBean.setSource(source);

        BeanDefinitionBuilder entryPointBuilder =
                BeanDefinitionBuilder.rootBeanDefinition(LoginUrlAuthenticationEntryPoint.class);
        entryPointBuilder.getRawBeanDefinition().setSource(source);
        entryPointBuilder.addConstructorArgValue(loginPage != null ? loginPage : DEF_LOGIN_PAGE);
        entryPointBuilder.addPropertyValue("portMapper", portMapper);
        entryPointBuilder.addPropertyValue("portResolver", portResolver);
        entryPointBean = (RootBeanDefinition) entryPointBuilder.getBeanDefinition();

        return null;
    }

    private RootBeanDefinition createFilterBean(String loginUrl, String defaultTargetUrl, String alwaysUseDefault,
            String loginPage, String authenticationFailureUrl, String successHandlerRef, String failureHandlerRef,
            String authDetailsSourceRef) {

        BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.rootBeanDefinition(filterClassName);

        if (!StringUtils.hasText(loginUrl)) {
            loginUrl = defaultLoginProcessingUrl;
        }

        this.loginProcessingUrl = loginUrl;

        BeanDefinitionBuilder matcherBuilder = BeanDefinitionBuilder.rootBeanDefinition("org.springframework.security.web.util.matcher.AntPathRequestMatcher");
        matcherBuilder.addConstructorArgValue(loginUrl);

        filterBuilder.addPropertyValue("requiresAuthenticationRequestMatcher", matcherBuilder.getBeanDefinition());

        if (StringUtils.hasText(successHandlerRef)) {
            filterBuilder.addPropertyReference("authenticationSuccessHandler", successHandlerRef);
        } else {
            BeanDefinitionBuilder successHandler = BeanDefinitionBuilder.rootBeanDefinition(SavedRequestAwareAuthenticationSuccessHandler.class);
            if ("true".equals(alwaysUseDefault)) {
                successHandler.addPropertyValue("alwaysUseDefaultTargetUrl", Boolean.TRUE);
            }
            successHandler.addPropertyValue("requestCache", requestCache);
            successHandler.addPropertyValue("defaultTargetUrl", StringUtils.hasText(defaultTargetUrl) ? defaultTargetUrl : DEF_FORM_LOGIN_TARGET_URL);
            filterBuilder.addPropertyValue("authenticationSuccessHandler", successHandler.getBeanDefinition());
        }

        if (StringUtils.hasText(authDetailsSourceRef)) {
            filterBuilder.addPropertyReference("authenticationDetailsSource", authDetailsSourceRef);
        }

        if (sessionStrategy != null) {
            filterBuilder.addPropertyValue("sessionAuthenticationStrategy", sessionStrategy);
        }

        if (StringUtils.hasText(failureHandlerRef)) {
            filterBuilder.addPropertyReference("authenticationFailureHandler", failureHandlerRef);
        } else {
            BeanDefinitionBuilder failureHandler = BeanDefinitionBuilder.rootBeanDefinition(SimpleUrlAuthenticationFailureHandler.class);
            if (!StringUtils.hasText(authenticationFailureUrl)) {
                // Fall back to redisplaying the custom login page, if one was specified.
                if (StringUtils.hasText(loginPage)) {
                    authenticationFailureUrl = loginPage;
                } else {
                    authenticationFailureUrl = DEF_FORM_LOGIN_AUTHENTICATION_FAILURE_URL;
                }
            }
            failureHandler.addPropertyValue("defaultFailureUrl", authenticationFailureUrl);
            failureHandler.addPropertyValue("allowSessionCreation", allowSessionCreation);
            filterBuilder.addPropertyValue("authenticationFailureHandler", failureHandler.getBeanDefinition());
        }

        return (RootBeanDefinition) filterBuilder.getBeanDefinition();
    }

    RootBeanDefinition getFilterBean() {
        return filterBean;
    }

    RootBeanDefinition getEntryPointBean() {
        return entryPointBean;
    }

    String getLoginPage() {
        return loginPage;
    }

    String getLoginProcessingUrl() {
        return loginProcessingUrl;
    }
}
