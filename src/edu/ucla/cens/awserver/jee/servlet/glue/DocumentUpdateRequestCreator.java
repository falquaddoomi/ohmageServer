package edu.ucla.cens.awserver.jee.servlet.glue;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import edu.ucla.cens.awserver.request.AwRequest;
import edu.ucla.cens.awserver.request.DocumentUpdateRequest;

public class DocumentUpdateRequestCreator implements AwRequestCreator {
	private static Logger _logger = Logger.getLogger(DocumentUpdateRequestCreator.class);
	
	/**
	 * Default constructor.
	 */
	public DocumentUpdateRequestCreator() {
		// Do nothing.
	}

	
	@Override
	public AwRequest createFrom(HttpServletRequest httpRequest) {
		_logger.info("Building document update request.");
		
		DocumentUpdateRequest request;
		try {
			request = (DocumentUpdateRequest) httpRequest.getAttribute("request");
		}
		catch(ClassCastException e) {
			throw new IllegalStateException("Invalid awRequest object in HTTPServlet. Must be DocumentUpdateRequest.");
		}
		if(request == null) {
			throw new IllegalStateException("Missing awRequest in HTTPServlet - Did the HTTPValidator run?");
		}
		
		return request;
	}
}