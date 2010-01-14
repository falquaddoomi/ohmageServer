package edu.ucla.cens.awserver.jee.servlet.glue;

import javax.servlet.http.HttpSession;

import edu.ucla.cens.awserver.datatransfer.AwRequest;

/**
 * Post-processor for merging authentication results into the user's HttpSession.
 * 
 * @author selsky
 */
public class AuthenticationHttpSessionModifier implements HttpSessionModifier {
//	private static Logger _logger = Logger.getLogger(AuthenticationHttpSessionModifier.class);
	
	/**
	 * Modifies the user's HttpSession based on a successful or failed authentication attempt.
	 */
	public void modifySession(AwRequest awRequest, HttpSession httpSession) {
		
		if(awRequest.getPayload().containsKey("failedRequest")) {
			
			httpSession.setAttribute("failedLogin", "true");
			
		} else {
			
			httpSession.setAttribute("userName", awRequest.getUser().getUserName());
			httpSession.setAttribute("isLoggedIn", "true");
			httpSession.setAttribute("subdomain", awRequest.getPayload().get("subdomain"));
			
			// remove previously failed login attempt -- removeAttribute() does nothing if no value is bound to the key
			httpSession.removeAttribute("failedLogin");
		}
	}
}