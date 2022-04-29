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
 

package org.maxkey.autoconfigure;

import org.maxkey.authn.AbstractAuthenticationProvider;
import org.maxkey.authn.SavedRequestAwareAuthenticationSuccessHandler;
import org.maxkey.authn.jwt.AuthJwtService;
import org.maxkey.authn.jwt.CongressService;
import org.maxkey.authn.jwt.InMemoryCongressService;
import org.maxkey.authn.jwt.RedisCongressService;
import org.maxkey.authn.provider.AuthenticationProviderFactory;
import org.maxkey.authn.provider.MobileAuthenticationProvider;
import org.maxkey.authn.provider.NormalAuthenticationProvider;
import org.maxkey.authn.provider.TrustedAuthenticationProvider;
import org.maxkey.authn.realm.AbstractAuthenticationRealm;
import org.maxkey.authn.session.SessionManager;
import org.maxkey.authn.session.SessionManagerFactory;
import org.maxkey.authn.support.rememberme.AbstractRemeberMeService;
import org.maxkey.authn.support.rememberme.JdbcRemeberMeService;
import org.maxkey.authn.web.HttpSessionListenerAdapter;
import org.maxkey.configuration.ApplicationConfig;
import org.maxkey.configuration.AuthJwkConfig;
import org.maxkey.constants.ConstsPersistence;
import org.maxkey.password.onetimepwd.AbstractOtpAuthn;
import org.maxkey.password.onetimepwd.OtpAuthnService;
import org.maxkey.password.onetimepwd.token.RedisOtpTokenStore;
import org.maxkey.persistence.MomentaryService;
import org.maxkey.persistence.redis.RedisConnectionFactory;
import org.maxkey.persistence.repository.LoginHistoryRepository;
import org.maxkey.persistence.repository.LoginRepository;
import org.maxkey.persistence.repository.PasswordPolicyValidator;
import org.maxkey.persistence.service.EmailSendersService;
import org.maxkey.persistence.service.SmsProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nimbusds.jose.JOSEException;


@Configuration
public class AuthenticationAutoConfiguration  implements InitializingBean {
    private static final  Logger _logger = 
            LoggerFactory.getLogger(AuthenticationAutoConfiguration.class);
    
    
    @Bean(name = "savedRequestSuccessHandler")
    public SavedRequestAwareAuthenticationSuccessHandler 
            savedRequestAwareAuthenticationSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler();
    }
    
    @Bean
    public AbstractAuthenticationProvider authenticationProvider(
    		AbstractAuthenticationProvider normalAuthenticationProvider,
    		AbstractAuthenticationProvider mobileAuthenticationProvider,
    		AbstractAuthenticationProvider trustedAuthenticationProvider
    		) {
    	AuthenticationProviderFactory authenticationProvider = new AuthenticationProviderFactory();
    	authenticationProvider.addAuthenticationProvider(normalAuthenticationProvider);
    	authenticationProvider.addAuthenticationProvider(mobileAuthenticationProvider);
    	authenticationProvider.addAuthenticationProvider(trustedAuthenticationProvider);
    	
    	return authenticationProvider;
    }
    		
    @Bean
    public AbstractAuthenticationProvider normalAuthenticationProvider(
    		AbstractAuthenticationRealm authenticationRealm,
    		ApplicationConfig applicationConfig,
    	    SessionManager sessionManager,
    	    AuthJwtService authJwtService
    		) {
    	_logger.debug("init authentication Provider .");
    	return new NormalAuthenticationProvider(
        		authenticationRealm,
        		applicationConfig,
        		sessionManager,
        		authJwtService
        	);
    }
    
    @Bean
    public AbstractAuthenticationProvider mobileAuthenticationProvider(
    		AbstractAuthenticationRealm authenticationRealm,
    		ApplicationConfig applicationConfig,
    	    OtpAuthnService otpAuthnService,
    	    SessionManager sessionManager
    		) {
    	_logger.debug("init Mobile authentication Provider .");
    	return new MobileAuthenticationProvider(
        		authenticationRealm,
        		applicationConfig,
        		otpAuthnService,
        		sessionManager
        	);
    }

    @Bean
    public AbstractAuthenticationProvider trustedAuthenticationProvider(
    		AbstractAuthenticationRealm authenticationRealm,
    		ApplicationConfig applicationConfig,
    	    SessionManager sessionManager
    		) {
    	_logger.debug("init Mobile authentication Provider .");
    	return new TrustedAuthenticationProvider(
        		authenticationRealm,
        		applicationConfig,
        		sessionManager
        	);
    }
    
    @Bean
    public AuthJwtService authJwtService(
    		AuthJwkConfig authJwkConfig,
    		RedisConnectionFactory redisConnFactory,
    		MomentaryService  momentaryService,
    		@Value("${maxkey.server.persistence}") int persistence) throws JOSEException {
    	CongressService congressService;
    	if (persistence == ConstsPersistence.REDIS) {
    		congressService = new RedisCongressService(redisConnFactory);
    	}else {
    		congressService = new InMemoryCongressService();
    	}
    	
    	AuthJwtService authJwtService = new AuthJwtService(authJwkConfig,congressService,momentaryService);
    	
    	return authJwtService;
    }
    
    @Bean(name = "otpAuthnService")
    public OtpAuthnService otpAuthnService(
            @Value("${maxkey.server.persistence}") int persistence,
            SmsProviderService smsProviderService,
            EmailSendersService emailSendersService,
            RedisConnectionFactory redisConnFactory) {
        OtpAuthnService otpAuthnService = 
        							new OtpAuthnService(smsProviderService,emailSendersService);
        
        if (persistence == ConstsPersistence.REDIS) {
            RedisOtpTokenStore redisOptTokenStore = new RedisOtpTokenStore(redisConnFactory);
            otpAuthnService.setRedisOptTokenStore(redisOptTokenStore);
        }
        
        _logger.debug("OneTimePasswordService {} inited." , 
        				persistence == ConstsPersistence.REDIS ? "Redis" : "InMemory");
        return otpAuthnService;
    }
    
    @Bean
    public PasswordPolicyValidator passwordPolicyValidator(JdbcTemplate jdbcTemplate,MessageSource messageSource) {
        return new PasswordPolicyValidator(jdbcTemplate,messageSource);
    }
    
    @Bean
    public LoginRepository loginRepository(JdbcTemplate jdbcTemplate) {
        return new LoginRepository(jdbcTemplate);
    }
    @Bean
    public LoginHistoryRepository loginHistoryRepository(JdbcTemplate jdbcTemplate) {
        return new LoginHistoryRepository(jdbcTemplate);
    }
    
    
    @Bean
    public SessionManager sessionManager(
            @Value("${maxkey.server.persistence}") int persistence,
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory redisConnFactory,
            @Value("${maxkey.session.timeout:1800}") int timeout
            ) {
    	_logger.trace("session timeout " + timeout);
        SessionManager  sessionManager  = 
                new SessionManagerFactory().getManager(
                		persistence, jdbcTemplate, redisConnFactory,timeout);
        return sessionManager;
    }
    
    
    /**
     * remeberMeService .
     * @return
     */
    @Bean
    public AbstractRemeberMeService remeberMeService(
            @Value("${maxkey.server.persistence}") int persistence,
            @Value("${maxkey.login.remeberme.validity}") int validity,
            ApplicationConfig applicationConfig,
            AuthJwtService authJwtService,
            JdbcTemplate jdbcTemplate) {
    	_logger.trace("init remeberMeService , validity {}." , validity);
        return new  JdbcRemeberMeService(
        		jdbcTemplate,applicationConfig,authJwtService,validity);
    }
    
    @Bean
    public HttpSessionListenerAdapter httpSessionListenerAdapter() {
        return new HttpSessionListenerAdapter();
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        
    }
}
