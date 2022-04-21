/*
 * Copyright [2022] [MaxKey of copyright http://www.maxkey.top]
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
 

package org.maxkey.authn.provider;

import org.maxkey.authn.LoginCredential;
import org.maxkey.authn.online.OnlineTicketService;
import org.maxkey.authn.realm.AbstractAuthenticationRealm;
import org.maxkey.configuration.ApplicationConfig;
import org.maxkey.constants.ConstsLoginType;
import org.maxkey.entity.UserInfo;
import org.maxkey.password.onetimepwd.AbstractOtpAuthn;
import org.maxkey.password.onetimepwd.OtpAuthnService;
import org.maxkey.web.WebConstants;
import org.maxkey.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;


/**
 * Mobile Authentication provider.
 * @author Crystal.Sea
 *
 */
public class MobileAuthenticationProvider extends NormalAuthenticationProvider {
	
    private static final Logger _logger =
            LoggerFactory.getLogger(MobileAuthenticationProvider.class);

    public String getProviderName() {
        return "mobile" + PROVIDER_SUFFIX;
    }
    

    public MobileAuthenticationProvider() {
		super();
	}


    public MobileAuthenticationProvider(
    		AbstractAuthenticationRealm authenticationRealm,
    		ApplicationConfig applicationConfig,
    	    OtpAuthnService otpAuthnService,
    	    OnlineTicketService onlineTicketServices) {
		this.authenticationRealm = authenticationRealm;
		this.applicationConfig = applicationConfig;
		this.otpAuthnService = otpAuthnService;
		this.onlineTicketServices = onlineTicketServices;
	}

    @Override
	public Authentication authenticate(LoginCredential loginCredential) {
		UsernamePasswordAuthenticationToken authenticationToken = null;
		_logger.debug("Trying to authenticate user '{}' via {}", 
                loginCredential.getPrincipal(), getProviderName());
        try {
        	
	        _logger.debug("authentication " + loginCredential);

	        emptyPasswordValid(loginCredential.getPassword());
	
	        emptyUsernameValid(loginCredential.getUsername());
	
	        UserInfo userInfo =  loadUserInfo(loginCredential.getUsername(),loginCredential.getPassword());
	
	        statusValid(loginCredential , userInfo);

	        //Validate PasswordPolicy
	        authenticationRealm.getPasswordPolicyValidator().passwordPolicyValid(userInfo);
	        
	        mobilecaptchaValid(loginCredential.getPassword(),userInfo);

	        //apply PasswordSetType and resetBadPasswordCount
	        authenticationRealm.getPasswordPolicyValidator().applyPasswordPolicy(userInfo);
	        
	        authenticationToken = createOnlineSession(loginCredential,userInfo);
	        // user authenticated
	        _logger.debug("'{}' authenticated successfully by {}.", 
	        		loginCredential.getPrincipal(), getProviderName());
	        
	        authenticationRealm.insertLoginHistory(userInfo, 
							        				ConstsLoginType.LOCAL, 
									                "", 
									                "xe00000004", 
									                WebConstants.LOGIN_RESULT.SUCCESS);
        } catch (AuthenticationException e) {
            _logger.error("Failed to authenticate user {} via {}: {}",
                    new Object[] {  loginCredential.getPrincipal(),
                                    getProviderName(),
                                    e.getMessage() });
            WebContext.setAttribute(
                    WebConstants.LOGIN_ERROR_SESSION_MESSAGE, e.getMessage());
        } catch (Exception e) {
            _logger.error("Login error Unexpected exception in {} authentication:\n{}" ,
                            getProviderName(), e.getMessage());
        }
       
        return  authenticationToken;
    }
    
    
    /**
     * mobile validate.
     * 
     * @param otpCaptcha String
     * @param authType   String
     * @param userInfo   UserInfo
     */
    protected void mobilecaptchaValid(String password, UserInfo userInfo) {
        // for mobile password
        if (applicationConfig.getLoginConfig().isMfa()) {
            UserInfo validUserInfo = new UserInfo();
            validUserInfo.setUsername(userInfo.getUsername());
            validUserInfo.setId(userInfo.getId());
            AbstractOtpAuthn smsOtpAuthn = otpAuthnService.getByInstId(userInfo.getInstId());
            if (password == null || !smsOtpAuthn.validate(validUserInfo, password)) {
                String message = WebContext.getI18nValue("login.error.captcha");
                _logger.debug("login captcha valid error.");
                throw new BadCredentialsException(message);
            }
        }
    }
  
}
