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
 

package org.maxkey.authn.support.rememberme;

import java.text.ParseException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.maxkey.authn.SignPrincipal;
import org.maxkey.authn.jwt.AuthJwtService;
import org.maxkey.configuration.ApplicationConfig;
import org.maxkey.crypto.jwt.HMAC512Service;
import org.maxkey.entity.UserInfo;
import org.maxkey.util.DateUtils;
import org.maxkey.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.nimbusds.jwt.JWTClaimsSet;

public abstract class AbstractRemeberMeService {
    private static final Logger _logger = LoggerFactory.getLogger(AbstractRemeberMeService.class);

    protected Integer validity = 7;

    protected ApplicationConfig applicationConfig;
    
    AuthJwtService authJwtService;

    // follow function is for persist
    public abstract void save(RemeberMe remeberMe);

    public abstract void update(RemeberMe remeberMe);

    public abstract RemeberMe read(RemeberMe remeberMe);

    public abstract void remove(String username);
    // end persist

    public String createRemeberMe(Authentication  authentication, 
    					HttpServletRequest request, HttpServletResponse response) {
        if (applicationConfig.getLoginConfig().isRemeberMe()) {
        	SignPrincipal principal = ((SignPrincipal)authentication.getPrincipal());
    		UserInfo userInfo = principal.getUserInfo();
            _logger.debug("Remeber Me ...");
            RemeberMe remeberMe = new RemeberMe();
            remeberMe.setId(WebContext.genId());
            remeberMe.setUserId(userInfo.getId());
            remeberMe.setUsername(userInfo.getUsername());
            remeberMe.setLastLoginTime(DateUtils.getCurrentDate());
            remeberMe.setExpirationTime(DateTime.now().plusDays(validity).toDate());
            save(remeberMe);
            _logger.debug("Remeber Me " + remeberMe);
            return genRemeberMe(remeberMe);
        }
        return null;
    }

    public String updateRemeberMe(RemeberMe remeberMe) {
        remeberMe.setLastLoginTime(new Date());
        remeberMe.setExpirationTime(DateTime.now().plusDays(validity).toDate());
        update(remeberMe);
        _logger.debug("update Remeber Me " + remeberMe);
        
        return genRemeberMe(remeberMe);
    }

    public boolean removeRemeberMe(HttpServletResponse response,UserInfo currentUser) {
        remove(currentUser.getUsername());

        return true;
    }
    
    public RemeberMe resolve(String rememberMeJwt) throws ParseException {
    	JWTClaimsSet claims = authJwtService.resolve(rememberMeJwt);
    	RemeberMe remeberMe = new RemeberMe();
		remeberMe.setId(claims.getJWTID());
		remeberMe.setUsername(claims.getSubject());
		return read(remeberMe);
    }
    
    public String genRemeberMe(RemeberMe remeberMe ) {
		_logger.debug("expiration Time : {}" , remeberMe.getExpirationTime());
		
		 JWTClaimsSet remeberMeJwtClaims =new  JWTClaimsSet.Builder()
				.issuer("")
				.subject(remeberMe.getUsername())
				.jwtID(remeberMe.getId())
				.issueTime(remeberMe.getLastLoginTime())
				.expirationTime(remeberMe.getExpirationTime())
				.claim("kid", HMAC512Service.MXK_AUTH_JWK)
				.build();
		
		return authJwtService.signedJWT(remeberMeJwtClaims);
	}

	public Integer getValidity() {
		return validity;
	}

	public void setValidity(Integer validity) {
		if(validity != 0 ) {
			this.validity = validity;
		}
	}
    

}
