/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.service;

import org.apache.log4j.Logger;
import org.ohmage.dao.Dao;
import org.ohmage.dao.DataAccessException;
import org.ohmage.request.AwRequest;
import org.ohmage.validator.AwRequestAnnotator;


/**
 * Checks the contents of a DAO to ensure that all of the information in the
 * XML is valid to add a new campaign.
 * 
 * @author John Jenkins
 */
public class CampaignAlreadyExistsValidationService extends AbstractAnnotatingDaoService {
	private static Logger _logger = Logger.getLogger(CampaignAlreadyExistsValidationService.class);
	
	/**
	 * Creates an annotating DAO object that will check the database for some
	 * information and, on error, will report the contents of the annotator.
	 * 
	 * @param annotator What should be reported if there is an error.
	 * 
	 * @param dao The DAO to use to access the database.
	 */
	public CampaignAlreadyExistsValidationService(AwRequestAnnotator annotator, Dao dao) {
		super(dao, annotator);
	}

	/**
	 * Calls the DAO and, if an error occurrs, reports it via the annotator.
	 */
	@Override
	public void execute(AwRequest awRequest) {
		_logger.info("Validating the contents of a campaign that is attempting to be created.");
		
		try {
			getDao().execute(awRequest);
			
			if(awRequest.isFailedRequest()) {
				getAnnotator().annotate(awRequest, "Campaign with the same URN already exists.");
			}
		}
		catch(DataAccessException dae) {
			throw new ServiceException(dae);
		}
	}

}