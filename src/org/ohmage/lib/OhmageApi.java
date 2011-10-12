package org.ohmage.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.annotator.Annotator;
import org.ohmage.domain.Document;
import org.ohmage.domain.MobilityPoint;
import org.ohmage.domain.ServerConfig;
import org.ohmage.domain.UserInformation;
import org.ohmage.domain.UserPersonal;
import org.ohmage.domain.campaign.SurveyResponse;
import org.ohmage.exception.ErrorCodeException;
import org.ohmage.lib.exception.ApiException;
import org.ohmage.lib.exception.RequestErrorException;
import org.ohmage.request.InputKeys;
import org.ohmage.request.Request;
import org.ohmage.request.RequestBuilder;
import org.ohmage.request.auth.AuthRequest;
import org.ohmage.request.auth.AuthTokenRequest;
import org.ohmage.request.document.DocumentCreationRequest;
import org.ohmage.request.mobility.MobilityReadRequest;
import org.ohmage.request.survey.SurveyResponseReadRequest;
import org.ohmage.util.StringUtils;
import org.ohmage.util.TimeUtils;

/**
 * This is the main interface class for the server.
 * 
 * @author John Jenkins
 */
public class OhmageApi {
	private static final int HTTP_PORT = 80;
	private static final int HTTPS_PORT = 443;
	
	private static final int CHUNK_SIZE = 4096;
	
	private static final String CONTENT_TYPE_HEADER = "Content-Type";
	private static final String CONTENT_TYPE_HTML = "text/html";
		
	private final URL url;
	
	/**
	 * REMOVE BEFORE RELEASE! THIS IS FOR TESTING PURPOSES ONLY.
	 * 
	 * @param args The arguments with which to test.
	 * 
	 * @throws Exception Catch-all for debugging.
	 */
	public static void main(String args[]) throws Exception {
		OhmageApi api = new OhmageApi("localhost", 8080, false);
		
		String authToken = api.getAuthenticationToken("sink.thaw", "mill.damper", "library");
		
		System.out.println(api.getUserInformation(authToken, "library").toJsonObject().toString(4));
		
		api.updateUser(authToken, "library", "sink.thaw", null, null, null, true, null, null, null, null, null, null);
		
		System.out.println(api.getUserInformation(authToken, "library").toJsonObject().toString(4));
	}
	
	/**
	 * DELETE ME!
	 * 
	 * @param filename The XML's filename.
	 * 
	 * @return The XML as a String.
	 * 
	 * @throws Exception If something fails.
	 *
	private static String readFile(final String filename) throws Exception {
		File file = new File(filename);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		InputStream is = new FileInputStream(file);
		int amountRead;
		while((amountRead = is.read(chunk)) != -1) {
			baos.write(chunk, 0, amountRead);
		}
		
		return new String(baos.toByteArray());
	}*/
	
	/**
	 * Creates a new OhmageAPI object that points to a single server.
	 * 
	 * @param serverAddress The servers address. This includes only the domain
	 * 						section of the URL such as "dev.andwellness.org" or
	 * 						"dev.mobilizingcs.org".
	 * 
	 * @param port The port to use instead of the standard HTTP and HTTPS 
	 * 			   ports. To use the standard ports, make this null.
	 * 
	 * @param secure If set, HTTPS will be used; otherwise, HTTP will be used.
	 * 				 If one is used and the server sends back a HTTP 301 or 302
	 * 				 status code, the call will be redirected to the 
	 * 				 appropriate protocol.
	 * 
	 * @throws IllegalArgumentException Thrown if the server address is null or
	 * 									not a valid address.
	 */
	public OhmageApi(final String serverAddress, final Integer port, 
			final boolean secure) {
		
		if(StringUtils.isEmptyOrWhitespaceOnly(serverAddress)) {
			throw new IllegalArgumentException("The server's address cannot be null.");
		}
		
		// Builds the server URL.
		StringBuilder serverUrlBuilder = new StringBuilder();
		
		// If we are dealing with secure connections,
		if(secure) {
			serverUrlBuilder.append("https://");
		}
		else {
			serverUrlBuilder.append("http://");
		}
		
		// Add the server's domain.
		serverUrlBuilder.append(serverAddress);
		
		// If a port was given, specify the destination.
		if((port != null) && ((port != HTTP_PORT) && (port != HTTPS_PORT))) {
			serverUrlBuilder.append(":").append(port);
		}
		
		try {
			url = new URL(serverUrlBuilder.toString());
		}
		catch(MalformedURLException e) {
			throw new IllegalArgumentException("The server's address is invalid.");
		}
	}
	
	/**************************************************************************
	 * Configuration Requests
	 *************************************************************************/
	
	/**
	 * Reads the server's configuration.
	 * 
	 * @return The server's configuration.
	 * 
	 * @throws ApiException Thrown if the server returns an error or if the
	 * 						server's configuration is invalid.
	 */
	public ServerConfig getServerConfiguration() throws ApiException {
		String serverResponse;
		try {
			serverResponse = 
				processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_CONFIG_READ), 
							new HashMap<String, Object>(0), 
							false), 
					InputKeys.DATA);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		
		try {
			return new ServerConfig(new JSONObject(serverResponse));
		}
		catch(JSONException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException(
					"The response is missing a required key.", e);
		}
	}
	
	/**************************************************************************
	 * Authentication Requests
	 *************************************************************************/

	/**
	 * Authenticates the username and password with the server and returns the
	 * user's hashed password to be used in subsequent calls.
	 * 
	 * @param username The user's username.
	 * 
	 * @param plaintextPassword The user's plaintext password.
	 * 
	 * @param client The client value.
	 * 
	 * @return The user's hashed password.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public String getHashedPassword(final String username, 
			final String plaintextPassword, final String client) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, plaintextPassword);
		parameters.put(InputKeys.CLIENT, client);

		try {
			return processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_USER_AUTH), 
							parameters, 
							false), 
					AuthRequest.KEY_HASHED_PASSWORD);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Authenticates the username and password with the server and returns an
	 * authentication token to be used in subsequent requests.
	 * 
	 * @param username The user's username.
	 * 
	 * @param plaintextPassword The user's plaintext password.
	 * 
	 * @param client The client value.
	 * 
	 * @return An authentication token.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public String getAuthenticationToken(final String username, 
			final String plaintextPassword, final String client) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, plaintextPassword);
		parameters.put(InputKeys.CLIENT, client);
		
		try {
			return processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_USER_AUTH_TOKEN), 
							parameters, 
							false), 
					AuthTokenRequest.KEY_AUTH_TOKEN);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}

	/**************************************************************************
	 * Document Requests
	 *************************************************************************/

	/**
	 * Creates a new document.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param name The document's name including an optional extension.
	 * 
	 * @param description An optional description for the document. Set this to
	 * 					  null to omit it.
	 * 
	 * @param privacyState The document's initial privacy state.
	 * 
	 * @param document A File object referencing the document to be uploaded.
	 * 
	 * @param campaignAndDocumentRoleMap A map of campaign IDs to document 
	 * 									 roles to be sent to the server. Either
	 * 									 this or 'classAndDocumentRoleMap' must
	 * 									 be non-null and not empty.
	 * 
	 * @param classAndDocumentRoleMap A map of class IDs to document roles to 
	 * 								  be sent to the server. Either this or
	 * 								  'campaignAndDocumentRoleMap' must be
	 * 								  non-null and not empty.
	 * 
	 * @return Returns the new document's unique identifier.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public String createDocument(final String authenticationToken, 
			final String client, final String name, final String description,
			final Document.PrivacyState privacyState,
			final File document,
			final Map<String, Document.Role> campaignAndDocumentRoleMap,
			final Map<String, Document.Role> classAndDocumentRoleMap)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.DOCUMENT_NAME, name);
		parameters.put(InputKeys.DESCRIPTION, description);
		parameters.put(InputKeys.PRIVACY_STATE, privacyState);
		parameters.put(InputKeys.DOCUMENT, document);
		
		if(campaignAndDocumentRoleMap != null) {
			parameters.put(
					InputKeys.DOCUMENT_CAMPAIGN_ROLE_LIST, 
					StringUtils.mapToStringList(
							campaignAndDocumentRoleMap, 
							InputKeys.ENTITY_ROLE_SEPARATOR, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(classAndDocumentRoleMap != null) {
			parameters.put(
					InputKeys.DOCUMENT_CLASS_ROLE_LIST, 
					StringUtils.mapToStringList(
							classAndDocumentRoleMap, 
							InputKeys.ENTITY_ROLE_SEPARATOR, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		
		try {
			return processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_DOCUMENT_CREATE), 
							parameters, 
							true), 
					DocumentCreationRequest.KEY_DOCUMENT_ID);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Retrieves the list of documents associated with the user based on the
	 * parameters.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param includePersonalDocuments Whether or not to include documents that
	 * 								   are only directly associated with the
	 * 								   user.
	 *  
	 * @param campaignIds The campaign IDs for which to get the documents'
	 * 					  information.
	 * 
	 * @param classIds The class IDs for which to get the documents' 
	 * 				   information.
	 * 
	 * @return A, possibly empty but never null, map of document unique 
	 * 		   identifiers to Document objects that contain all of the data
	 * 		   pertaining to the document.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public Map<String, Document> getDocuments(final String authenticationToken,
			final String client, final boolean includePersonalDocuments,
			final Collection<String> campaignIds, 
			final Collection<String> classIds) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.DOCUMENT_PERSONAL_DOCUMENTS, includePersonalDocuments);
		
		if(campaignIds != null) {
			parameters.put(InputKeys.CAMPAIGN_URN_LIST, StringUtils.collectionToStringList(campaignIds, InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(classIds != null) {
			parameters.put(InputKeys.CLASS_URN_LIST, StringUtils.collectionToStringList(classIds, InputKeys.LIST_ITEM_SEPARATOR));
		}
		
		JSONObject documents;
		try {
			documents = new JSONObject(
					processJsonResponse(
						makeRequest(
								new URL(url.toString() + RequestBuilder.API_DOCUMENT_READ), 
								parameters, 
								false), 
						Request.JSON_KEY_DATA
					)
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		catch(JSONException e) {
			throw new ApiException("The data was not proper JSON.", e);
		}
		
		Map<String, Document> result = new HashMap<String, Document>(documents.length());
		
		Iterator<?> keys = documents.keys();
		while(keys.hasNext()) {
			String documentId = (String) keys.next();
			
			try {
				result.put(documentId, new Document(documentId, documents.getJSONObject(documentId)));
			}
			catch(JSONException e) {
				throw new ApiException("The document was not proper JSON: " + documentId, e);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the contents of the document.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param documentId The document's unique identifier.
	 * 
	 * @return The contents of the document.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public byte[] getDocumentContents(final String authenticationToken,
			final String client, final String documentId) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.DOCUMENT_ID, documentId);
		
		try {
			return makeRequest(
					new URL(url.toString() + RequestBuilder.API_DOCUMENT_READ_CONTENTS), 
					parameters, 
					false);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Updates the information and/or contents of a document. The 
	 * authentication and document ID are required, but any of the other values
	 * may be null meaning that their values won't be updated.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param documentId The document's unique identifier.
	 * 
	 * @param newName The document's new name.
	 * 
	 * @param newDescription The document's new description.
	 * 
	 * @param newPrivacyState The document's new privacy state.
	 * 
	 * @param newContents The document's new contents.
	 * 
	 * @param campaignAndRolesToAdd A map of campaign IDs to document roles to
	 * 								associate with the document.
	 * 
	 * @param campaignsToRemove A list of the campaigns whose association with
	 * 							the document should be removed.
	 * 
	 * @param classAndRolesToAdd A map of class IDs to document roles to
	 * 							 associate with the document.
	 * 
	 * @param classesToRemove A list of the classes whose association with the
	 * 						  document should be removed.
	 * 
	 * @param userAndRolesToAdd A map of usernames to document roles to 
	 * 							associate with the document.
	 * 
	 * @param usersToRemove A list of usernames of users whose association with
	 * 						the document should be removed.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void updateDocument(final String authenticationToken,
			final String client, final String documentId,
			final String newName, final String newDescription,
			final Document.PrivacyState newPrivacyState, 
			final byte[] newContents,
			final Map<String, Document.Role> campaignAndRolesToAdd,
			final Collection<String> campaignsToRemove,
			final Map<String, Document.Role> classAndRolesToAdd,
			final Collection<String> classesToRemove,
			final Map<String, Document.Role> userAndRolesToAdd,
			final Collection<String> usersToRemove) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.DOCUMENT_ID, documentId);
		parameters.put(InputKeys.DOCUMENT_NAME, newName);
		parameters.put(InputKeys.DESCRIPTION, newDescription);
		parameters.put(InputKeys.PRIVACY_STATE, newPrivacyState);
		parameters.put(InputKeys.DOCUMENT, newContents);
		
		if(campaignAndRolesToAdd != null) {
			parameters.put(
					InputKeys.DOCUMENT_CAMPAIGN_ROLE_LIST, 
					StringUtils.mapToStringList(
							campaignAndRolesToAdd, 
							InputKeys.ENTITY_ROLE_SEPARATOR, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(campaignsToRemove != null) {
			parameters.put(
					InputKeys.CAMPAIGN_LIST_REMOVE, 
					StringUtils.collectionToStringList(
							campaignsToRemove, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(classAndRolesToAdd != null) {
			parameters.put(
					InputKeys.DOCUMENT_CLASS_ROLE_LIST, 
					StringUtils.mapToStringList(
							classAndRolesToAdd, 
							InputKeys.ENTITY_ROLE_SEPARATOR, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(classesToRemove != null) {
			parameters.put(
					InputKeys.CLASS_LIST_REMOVE, 
					StringUtils.collectionToStringList(
							classesToRemove, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(userAndRolesToAdd != null) {
			parameters.put(
					InputKeys.DOCUMENT_USER_ROLE_LIST, 
					StringUtils.mapToStringList(
							userAndRolesToAdd, 
							InputKeys.ENTITY_ROLE_SEPARATOR, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(usersToRemove != null) {
			parameters.put(
					InputKeys.USER_LIST_REMOVE, 
					StringUtils.collectionToStringList(
							usersToRemove, 
							InputKeys.LIST_ITEM_SEPARATOR));
		}
		
		try {
			processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_DOCUMENT_UPDATE), 
							parameters, 
							true),
					null);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Deletes a document.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param documentId The unique identifier for the document to be deleted.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void deleteDocument(final String authenticationToken,
			final String client, final String documentId)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.DOCUMENT_ID, documentId);
		
		try {
			processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_DOCUMENT_DELETE), 
							parameters, 
							false), 
					null);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**************************************************************************
	 * Mobility Requests
	 *************************************************************************/
	
	/**
	 * Uploads a collection of Mobility points.
	 * 
	 * @param username The username of the user who is attempting the upload.
	 * 
	 * @param hashedPassword The user's hashed password.
	 * 
	 * @param client The client value.
	 * 
	 * @param points The collection of points to be uploaded.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void uploadMobilityPoints(final String username, 
			final String hashedPassword, final String client, 
			final Collection<MobilityPoint> points) 
			throws ApiException, RequestErrorException {

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, hashedPassword);
		parameters.put(InputKeys.CLIENT, client);
		
		JSONArray dataArray = new JSONArray();
		for(MobilityPoint point : points) {
			if(point == null) {
				continue;
			}
			
			JSONObject pointJson = point.toJson(false, true);
			if(pointJson == null) {
				throw new ApiException("One of the Mobility points could not be converted to JSON.");
			}
			
			dataArray.put(pointJson);
		}
		parameters.put(InputKeys.DATA, dataArray);
		
		try {
			processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_MOBILITY_UPLOAD), 
							parameters, 
							false), 
					null);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Reads Mobility points.
	 * 
	 * @param authenticationToken The authentication token for the user making
	 * 							  the request. This may be null if a username
	 * 							  and password are provided.
	 * 
	 * @param username The username of the user that is making the request. 
	 * 				   This may be null if the authentication token is 
	 * 				   provided.
	 * 
	 * @param password The hashed password of the user that is making the 
	 * 				   request. This may be null if the authentication token is
	 * 				   provided.
	 * 
	 * @param client The client value.
	 * 
	 * @param date The date for which the Mobility points will be gathered.
	 * 
	 * @return A, possibly empty but never null, list of MobilityInformation
	 * 		   objects.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public List<MobilityPoint> readMobilityPoints(final String authenticationToken, 
			final String username, final String password, final String client,
			final Date date) throws ApiException, RequestErrorException {

		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, password);
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		
		if(date != null) {
			parameters.put(InputKeys.DATE, TimeUtils.getIso8601DateString(date));
		}
		
		JSONArray response;
		try {
			response = new JSONArray(
					processJsonResponse(
							makeRequest(
									new URL(url.toString() + RequestBuilder.API_MOBILITY_READ), 
									parameters, 
									false), 
							MobilityReadRequest.JSON_KEY_DATA
						)
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		catch(JSONException e) {
			throw new ApiException("The response body was not proper JSON.", e);
		}
		
		int numResults = response.length();
		List<MobilityPoint> results = new ArrayList<MobilityPoint>(numResults);
		
		for(int i = 0; i < numResults; i++) {
			JSONObject currResult = response.optJSONObject(i);
			
			// All Mobility points from the server are "mode_only", but that is
			// not included in the response. We add it here for the 
			// constructor's benefit.
			try {
				currResult.put(MobilityPoint.JSON_KEY_SUBTYPE, MobilityPoint.SubType.MODE_ONLY);
			}
			catch(JSONException e) {
				throw new ApiException("Error adding the subtype to the response.", e);
			}
			
			try {
				results.add(new MobilityPoint(currResult, MobilityPoint.PrivacyState.PRIVATE));
			}
			catch(ErrorCodeException e) {
				throw new ApiException("The server returned an malformed MobilityInformation object.", e);
			}
		}
		
		return results;
	}
	
	/**************************************************************************
	 * Survey Response Requests
	 *************************************************************************/
	
	/**
	 * Uploads a collection of survey responses.
	 * 
	 * @param username The username of the user for whom this survey response
	 * 				   belongs.
	 * 
	 * @param hashedPassword The hashsed password of the user that is creating
	 * 						 this point.
	 * 
	 * @param client The client value.
	 * 
	 * @param campaignId The unique identifier for the campaign for whom these
	 * 					 survey responses belong.
	 * 
	 * @param campaignCreationTimestamp The campaign's creation timestamp to
	 * 									ensure we are not uploading out-dated
	 * 									data.
	 * 
	 * @param surveyResponses The collection of survey responses to be 
	 * 						  uploaded.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void uploadSurveyResponses(final String username, 
			final String hashedPassword, final String client,
			final String campaignId, final Date campaignCreationTimestamp,
			final Collection<SurveyResponse> surveyResponses)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, hashedPassword);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.CAMPAIGN_URN, campaignId);
		parameters.put(InputKeys.CAMPAIGN_CREATION_TIMESTAMP, TimeUtils.getIso8601DateTimeString(campaignCreationTimestamp));
		
		JSONArray dataArray = new JSONArray();
		for(SurveyResponse response : surveyResponses) {
			if(response == null) {
				continue;
			}
			
			JSONObject responseJson = response.toJson(false, false, false, 
					false, true, true, true, true, true, true, false, false, 
					true, true, true, false);
			if(responseJson == null) {
				throw new ApiException("One of the survey responses could not be converted to JSON.");
			}
			
			dataArray.put(responseJson);
		}
		parameters.put(InputKeys.SURVEYS, dataArray);
		
		try {
			processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_SURVEY_UPLOAD), 
							parameters, 
							true), 
					null);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}	
	
	/**
	 * Makes a request to the server for the survey responses based on the 
	 * given parameters and returns the CSV file as a byte array.
	 * 
	 * @param authenticationToken The user's current authentication token. This
	 * 							  may be null if a username and password are 
	 * 							  given.
	 * 
	 * @param username The user's username. This may be null if an 
	 * 				   authentication token is given.
	 * 
	 * @param hashedPassword The user's hashed password. This may be null if an 
	 * 						 authentication token was given.
	 * 
	 * @param client The client value.
	 * 
	 * @param campaignId The campaign's unique identifier.
	 * 
	 * @param usernames A collection of usernames for which the results will 
	 * 					only apply to those users.
	 * 
	 * @param columnList A collection of column IDs which will limit the 
	 * 					 results to only returning that specific type of 
	 * 					 information.
	 * 
	 * @param surveyIdList A collection of campaign-unique survey IDs which 
	 * 					   will limit the results to only those responses from
	 * 					   these surveys.
	 * 
	 * @param promptIdList A collection of campaign-unique prompt IDs which 
	 * 					   will limit the results to only those responses from
	 * 					   these prompts.
	 * 
	 * @param collapse Whether or not to collapse the results which will result
	 * 				   in duplicate rows being rolled into a single resulting
	 * 				   row.
	 * 
	 * @param startDate A date and time limiting the results to only those that
	 * 					occurred on or after this date.
	 * 
	 * @param endDate A date and time limiting the results to only those that
	 * 				  occurred on or before this date.
	 * 
	 * @param privacyState A survey response privacy state limiting the results
	 * 					   to only those with the given privacy state.
	 * 
	 * @param returnId Whether or not to return unique identifiers for the 
	 * 				   survey responses.
	 * 
	 * @return A byte array representing the CSV file.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public byte[] getSurveyResponsesCsv(
			final String authenticationToken, final String username, 
			final String hashedPassword, final String client,
			final String campaignId, final Collection<String> usernames,
			final Collection<SurveyResponse.ColumnKey> columnList,
			final Collection<String> surveyIdList, 
			final Collection<String> promptIdList,
			final Boolean collapse,
			final Date startDate, final Date endDate, 
			final SurveyResponse.PrivacyState privacyState,
			final Boolean returnId)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, hashedPassword);
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.CAMPAIGN_URN, campaignId);
		parameters.put(InputKeys.OUTPUT_FORMAT, SurveyResponse.OutputFormat.CSV);
		
		if(usernames != null) {
			if(usernames.size() == 0) {
				parameters.put(InputKeys.USER_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.USER_LIST, StringUtils.collectionToStringList(usernames, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		
		if(columnList != null) {
			if(columnList.size() == 0) {
				parameters.put(InputKeys.COLUMN_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.COLUMN_LIST, StringUtils.collectionToStringList(columnList, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		
		if(surveyIdList != null) {
			if(surveyIdList.size() == 0) {
				parameters.put(InputKeys.SURVEY_ID_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.SURVEY_ID_LIST, StringUtils.collectionToStringList(surveyIdList, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		if(promptIdList != null) {
			if(promptIdList.size() == 0) {
				parameters.put(InputKeys.PROMPT_ID_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.PROMPT_ID_LIST, StringUtils.collectionToStringList(promptIdList, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		
		parameters.put(InputKeys.COLLAPSE, collapse);
		//parameters.put(InputKeys.SUPPRESS_METADATA, true);
		parameters.put(InputKeys.START_DATE, TimeUtils.getIso8601DateTimeString(startDate));
		parameters.put(InputKeys.END_DATE, TimeUtils.getIso8601DateTimeString(endDate));
		parameters.put(InputKeys.RETURN_ID, returnId);
		
		if(privacyState != null) {
			parameters.put(InputKeys.PRIVACY_STATE, privacyState.toString());
		}
		
		byte[] response;
		try {
			response = makeRequest(
					new URL(url.toString() + RequestBuilder.API_SURVEY_RESPONSE_READ), 
					parameters, 
					false
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		
		return response;
	}
	
	public /*Collection<SurveyResponse>*/JSONObject getSurveyResponsesJsonColumns(
			final String authenticationToken, final String username, 
			final String hashedPassword, final String client,
			final String campaignId, final Collection<String> usernames,
			final Collection<String> columnList,
			final Collection<String> surveyIdList, 
			final Collection<String> promptIdList,
			final Boolean collapse,
			final Date startDate, final Date endDate, 
			final SurveyResponse.PrivacyState privacyState,
			final Boolean returnId)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.USER, username);
		parameters.put(InputKeys.PASSWORD, hashedPassword);
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.CAMPAIGN_URN, campaignId);
		parameters.put(InputKeys.OUTPUT_FORMAT, SurveyResponse.OutputFormat.JSON_COLUMNS);
		parameters.put(InputKeys.USER_LIST, StringUtils.collectionToStringList(usernames, InputKeys.LIST_ITEM_SEPARATOR));
		
		if(columnList != null) {
			if(columnList.size() == 0) {
				parameters.put(InputKeys.COLUMN_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.COLUMN_LIST, StringUtils.collectionToStringList(columnList, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		
		if(surveyIdList != null) {
			if(surveyIdList.size() == 0) {
				parameters.put(InputKeys.SURVEY_ID_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.SURVEY_ID_LIST, StringUtils.collectionToStringList(surveyIdList, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		if(promptIdList != null) {
			if(promptIdList.size() == 0) {
				parameters.put(InputKeys.PROMPT_ID_LIST, SurveyResponseReadRequest.URN_SPECIAL_ALL);
			}
			else {
				parameters.put(InputKeys.PROMPT_ID_LIST, StringUtils.collectionToStringList(promptIdList, InputKeys.LIST_ITEM_SEPARATOR));
			}
		}
		
		parameters.put(InputKeys.COLLAPSE, collapse);
		parameters.put(InputKeys.SUPPRESS_METADATA, true);
		parameters.put(InputKeys.START_DATE, TimeUtils.getIso8601DateTimeString(startDate));
		parameters.put(InputKeys.END_DATE, TimeUtils.getIso8601DateTimeString(endDate));
		parameters.put(InputKeys.RETURN_ID, returnId);
		
		if(privacyState != null) {
			parameters.put(InputKeys.PRIVACY_STATE, privacyState.toString());
		}
		
		JSONObject response;
		try {
			response = new JSONObject(
					processJsonResponse(
							makeRequest(
									new URL(url.toString() + RequestBuilder.API_SURVEY_RESPONSE_READ), 
									parameters, 
									false
								),
							InputKeys.DATA
						)
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		catch(JSONException e) {
			throw new ApiException("The response was proper JSON but the data was not.", e);
		}
		
		
		//return null;
		return response;
	}
	
	/**************************************************************************
	 * User Requests
	 *************************************************************************/
	
	/**
	 * Creates a new user. The requesting user must be an admin.
	 * 
	 * @param authenticationToken The requesting user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param newUsername The username for the new user.
	 * 
	 * @param newPassword The plaintext password for the new user.
	 * 
	 * @param admin Whether or not the new user should be an admin.
	 * 
	 * @param enabled Whether or not the new user's account should be enabled.
	 * 
	 * @param newAccount Whether or not the new user must change their password
	 * 					 before they can login. This is optional and defaults 
	 * 					 to true. To omit this value, pass null.
	 * 
	 * @param canCreateCampaigns Whether or not the new user is allowed to 
	 * 							 create campaigns. This is optional and 
	 * 							 defaults to however the server is configured.
	 * 							 To omit this value, pass null.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void createUser(final String authenticationToken, 
			final String client,
			final String newUsername, final String newPassword,
			final boolean admin, final boolean enabled,
			final Boolean newAccount, final Boolean canCreateCampaigns) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.USERNAME, newUsername);
		parameters.put(InputKeys.PASSWORD, newPassword);
		parameters.put(InputKeys.USER_ADMIN, admin);
		parameters.put(InputKeys.USER_ENABLED, enabled);
		parameters.put(InputKeys.NEW_ACCOUNT, newAccount);
		parameters.put(InputKeys.CAMPAIGN_CREATION_PRIVILEGE, canCreateCampaigns);
		
		try {
			processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_USER_CREATE),
							parameters,
							false
					),
					null
			);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Retrieves the personal information about each of the users in all of the
	 * given classes and campaigns.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param campaignIds A collection of campaign IDs whose users' personal
	 * 					  information is desired.
	 * 
	 * @param classIds A collection of class DIs whose users' personal 
	 * 				   information is desired.
	 * 
	 * @return A, possibly empty but never null, map of usernames to 
	 * 		   UserPersonal information for the user or null if that user 
	 * 		   doesn't have personal information.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public Map<String, UserPersonal> getUsersPersonalInformation(
			final String authenticationToken, final String client,
			final Collection<String> campaignIds, 
			final Collection<String> classIds)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		
		if(campaignIds != null) {
			parameters.put(InputKeys.CAMPAIGN_URN_LIST, StringUtils.collectionToStringList(campaignIds, InputKeys.LIST_ITEM_SEPARATOR));
		}
		if(classIds != null) {
			parameters.put(InputKeys.CLASS_URN_LIST, StringUtils.collectionToStringList(classIds, InputKeys.LIST_ITEM_SEPARATOR));
		}
		
		JSONObject response;
		try {
			response = new JSONObject(
					processJsonResponse(
							makeRequest(
									new URL(url.toString() + RequestBuilder.API_USER_READ), 
									parameters, 
									false
								),
							InputKeys.DATA
						)
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		catch(JSONException e) {
			throw new ApiException("The response was proper JSON but the data was not.", e);
		}
		
		Map<String, UserPersonal> result = new HashMap<String, UserPersonal>(response.length());
		
		Iterator<?> keys = response.keys();
		while(keys.hasNext()) {
			String username = (String) keys.next();
			
			try {
				JSONObject information = response.getJSONObject(username);
			
				if(information.length() == 0) {
					result.put(username, null);
				}
				else {
					result.put(username, new UserPersonal(information));
				}
			} 
			catch(JSONException e) {
				throw new ApiException("The user personal information was not well-formed JSON.");
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the user information about the currently logged in user.
	 * 
	 * @param authenticationToken The user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @return A UserInformation object about this user.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public UserInformation getUserInformation(final String authenticationToken,
			final String client) throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		
		JSONObject response;
		try {
			response = new JSONObject(
					processJsonResponse(
							makeRequest(
									new URL(url.toString() + RequestBuilder.API_USER_INFO_READ), 
									parameters, 
									false
								),
							InputKeys.DATA
						)
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
		catch(JSONException e) {
			throw new ApiException("The response was proper JSON but the data was not.", e);
		}

		if(response.length() == 0) {
			return null;
		}
		else if(response.length() > 1) {
			throw new ApiException("Multiple user's information was returned.");
		}

		try {
			return new UserInformation(response.getJSONObject((String) response.keys().next()));
		} 
		catch(JSONException e) {
			throw new ApiException("The user's information was not well-formed JSON.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The user's information is missing some information.", e);
		}
	}
	
	/**
	 * Updates a user's information.
	 * 
	 * @param authenticationToken The user's authentication token. Required.
	 * 
	 * @param client The client value. Required.
	 * 
	 * @param username The username of the user to be updated. Required.
	 * 
	 * @param admin Whether or not the user should be an admin. Optional.
	 * 
	 * @param enabled Whether or not the user's account should be enabled.
	 * 				  Optional.
	 * 
	 * @param newAccount Whether or not the user should be forced to change 
	 * 					 their password the next time they login. Optional.
	 * 
	 * @param campaignCreationPrivilege Whether or not the usr is allowed to
	 * 									create campaigns. Optional.
	 * 
	 * @param firstName The user's new first name. Optional.
	 * 
	 * @param lastName The user's new last name. Optional.
	 * 
	 * @param organization The user's new organization. Optional.
	 * 
	 * @param personalId The user's new personal identifier. Optional.
	 * 
	 * @param emailAddress The user's email address. Optional.
	 * 
	 * @param jsonData The user's new JSON data. It is advised to get the old
	 * 				   JSON data and update accordingly. Optional.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void updateUser(final String authenticationToken, 
			final String client, final String username, 
			final Boolean admin, final Boolean enabled, 
			final Boolean newAccount, final Boolean campaignCreationPrivilege,
			final String firstName, final String lastName,
			final String organization, final String personalId,
			final String emailAddress, final JSONObject jsonData)
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(InputKeys.USERNAME, username);
		parameters.put(InputKeys.USER_ADMIN, admin);
		parameters.put(InputKeys.USER_ENABLED, enabled);
		parameters.put(InputKeys.NEW_ACCOUNT, newAccount);
		parameters.put(InputKeys.CAMPAIGN_CREATION_PRIVILEGE, campaignCreationPrivilege);
		parameters.put(InputKeys.FIRST_NAME, firstName);
		parameters.put(InputKeys.LAST_NAME, lastName);
		parameters.put(InputKeys.ORGANIZATION, organization);
		parameters.put(InputKeys.PERSONAL_ID, personalId);
		parameters.put(InputKeys.EMAIL_ADDRESS, emailAddress);
		parameters.put(InputKeys.USER_JSON_DATA, jsonData);
		
		try {
			makeRequest(
					new URL(url.toString() + RequestBuilder.API_USER_UPDATE), 
					parameters, 
					false
				);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**
	 * Deletes a user. The requesting user must be an admin.
	 * 
	 * @param authenticationToken The requesting user's authentication token.
	 * 
	 * @param client The client value.
	 * 
	 * @param usernames A collection of usernames of the users to be deleted.
	 * 
	 * @throws ApiException Thrown if there is a library error.
	 * 
	 * @throws RequestErrorException Thrown if the server returns an error.
	 */
	public void deleteUser(final String authenticationToken,
			final String client, 
			final Collection<String> usernames) 
			throws ApiException, RequestErrorException {
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(InputKeys.AUTH_TOKEN, authenticationToken);
		parameters.put(InputKeys.CLIENT, client);
		parameters.put(
				InputKeys.USER_LIST, 
				StringUtils.collectionToStringList(
						usernames, 
						InputKeys.LIST_ITEM_SEPARATOR
					)
			);
		
		try {
			processJsonResponse(
					makeRequest(
							new URL(url.toString() + RequestBuilder.API_USER_DELETE),
							parameters,
							false
					),
					null
			);
		}
		catch(MalformedURLException e) {
			throw new ApiException("The URL was incorrectly created.", e);
		}
		catch(IllegalArgumentException e) {
			throw new ApiException("The response was not proper JSON.", e);
		}
	}
	
	/**************************************************************************
	 * Private Methods
	 *************************************************************************/
	
	/**
	 * Makes a call to the URL. The call will be a GET if 'postParameters' is
	 * null and a POST if 'postParameters' is non-null, even if it is empty. If
	 * it is a POST, 'isForm' will set it to be a "multipart/form-data" 
	 * request, but if it is set to false it will default to a 
	 * "application/x-www-form-urlencoded" request.<br />
	 * <br />
	 * If the response has a Content-Type that suggests that it is JSON, it 
	 * will check if the ohmage result is success or failure and throw an 
	 * exception if it is failure.
	 * 
	 * @param url The URL which dictates the location to which the request
	 * 			  should be made. This includes the scheme, which must be an
	 * 			  HTTP scheme ("http" or "https"), a domain, an optional port,
	 * 			  a path, and an optional query string. 
	 * 
	 * @param postParameters A map of keys to values for a POST call. The 
	 * 						 values must be a byte array to facilitate sending
	 * 						 things other than text such as images. If this is
	 * 						 null, the request will be a GET request; if this
	 * 						 is non-null, even if it is empty, it will force it
	 * 						 to be a POST call.
	 * 
	 * @param isForm This flag is used if the 'postParameters' is non-null and
	 * 				 indicates whether this POST should be a 
	 * 				 "multipart/form-data" request or not.
	 * 
	 * @return Returns the result from the server as a byte array. 
	 * 
	 * @throws ApiException Thrown if the URL is not an HTTP URL or if there
	 * 						   was an error communicating with the server.
	 */
	private byte[] makeRequest(final URL url, 
			final Map<String, Object> postParameters, final boolean isForm) 
		throws ApiException, RequestErrorException {
		
		// Create a default client to use to connect with.
		HttpClient httpClient = new DefaultHttpClient();
		
		// Build the request based on the parameters.
		HttpRequestBase request;
		if(postParameters == null) {
			// This is a GET request and the parameters are encoded in the URL.
			try {
				request = new HttpGet(url.toURI());
			}
			catch(URISyntaxException e) {
				throw new ApiException("There was an error building the request.", e);
			}
		}
		else {
			// This is a POST request and the parameter list must be built.
			try {
				HttpPost postRequest = new HttpPost(url.toURI());
				request = postRequest;
				
				// Build the parameter map which is called a "HttpEntity".
				HttpEntity entity;
				// It is a "multipart/form-data" request and the parameters
				// must be assigned to the 'entity'.
				if(isForm) {
					MultipartEntity multipartEntity = new MultipartEntity();
					entity = multipartEntity;
					
					for(String key : postParameters.keySet()) {
						Object value = postParameters.get(key);
						
						// If it is a File, add it to the entity list as an 
						// attachment.
						if(value instanceof File) {
							multipartEntity.addPart(key, new FileBody((File) value));
						}
						// Otherwise, get its string value and add it as such.
						else if(value != null) {
							try {
								multipartEntity.addPart(key, new StringBody(String.valueOf(value)));
							}
							catch(UnsupportedEncodingException e) {
								throw new ApiException("The value for key '" + key + "' could not be encoded.", e);
							}
						}
					}
				}
				// It is a "application/x-www-form-urlencoded" request and the
				// parameters can each be added as a string-string pair.
				else {
					List<BasicNameValuePair> items = new ArrayList<BasicNameValuePair>(postParameters.size());
					
					for(String key : postParameters.keySet()) {
						Object value = postParameters.get(key);
						
						if(value != null) {
							items.add(new BasicNameValuePair(key, String.valueOf(value)));
						}
					}
					
					try {
						entity = new UrlEncodedFormEntity(items);
					}
					catch(UnsupportedEncodingException e) {
						throw new ApiException("The parameter list could not be properly encoded.", e);
					}
				}
				postRequest.setEntity(entity);
			}
			catch(URISyntaxException e) {
				throw new ApiException("There was an error building the request.", e);
			}
		}
		
		// Make the request and get the response.
		HttpResponse httpResponse;
		try {
			httpResponse = httpClient.execute(request);
		}
		catch(ClientProtocolException e) {
			throw new ApiException("An HTTP protocol error occurred.", e);
		}
		catch(IOException e) {
			throw new ApiException("The connection was aborted.", e);
		}
		
		// Check the status code.
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		// If it is a redirect, get the new location and remake the request.
		if((statusCode == 301) || (statusCode == 302)) {
			String newLocation = httpResponse.getFirstHeader("Location").getValue();
			
			try {
				return makeRequest(new URL(newLocation), postParameters, isForm);
			}
			catch(MalformedURLException e) {
				throw new ApiException("The server returned a bad redirect address: " + newLocation, e);
			}
		}
		// Otherwise, if it is is a non-success code, fail the request.
		else if(statusCode != 200) {
			throw new ApiException("There was an error connecting to the server: " + statusCode);
		}
		
		// Retrieve the server's response as an InputStream.
		InputStream content;
		try {
			content = httpResponse.getEntity().getContent();
		}
		catch(IOException e) {
			throw new ApiException("There was an error connecting to the response from the server.", e);
		}
		
		// Read the results as a byte array. This is used instead of a string 
		// to allow the function to me more open to different types of return 
		// values such as text, images, etc.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] chunk = new byte[CHUNK_SIZE];
		int amountRead;
		try {
			while((amountRead = content.read(chunk)) != -1) {
				baos.write(chunk, 0, amountRead);
			}
		}
		catch(IOException e) {
			throw new ApiException("There was an error reading from the server.", e);
		}
		byte[] result = baos.toByteArray();
		
		// Finally, check the Content-Type to see if it suggests that this is
		// an ohmage JSON result. If so, check if it failed and, if so, throw
		// an exception.
		Header[] headers = httpResponse.getHeaders(CONTENT_TYPE_HEADER);
		String contentType = headers[0].getValue();
		if(CONTENT_TYPE_HTML.equals(contentType)) {
			checkFailure(result);
		}
		
		// Return the byte array.
		return result;
	}

	/**
	 * Throws an exception if the response is valid ohmage JSON and indicates
	 * failure.
	 * 
	 * @param response The byte array response returned by the server.
	 * 
	 * @throws IllegalArgumentException Thrown if the response is valid ohmage
	 * 									JSON and indicates failure, but the 
	 * 									error code and error text are missing.
	 * 
	 * @throws RequestErrorException Thrown if the response is valid ohmage 
	 * 								 JSON and indicates failure.
	 */
	private void checkFailure(final byte[] response) 
			throws RequestErrorException {
		
		JSONObject jsonResponse;
		try {
			jsonResponse = new JSONObject(new String(response));
		}
		catch(JSONException e) {
			// If it isn't JSON, we were mistaken and can abort.
			return;
		}
		
		try {
			// If it is flagged as failed, we need to throw an exception.
			if(Request.RESULT_FAILURE.equals(
					jsonResponse.getString(Request.JSON_KEY_RESULT))) {
				
				// Generate an ohmage-specific error.
				try {
					JSONObject error = jsonResponse.getJSONArray(
							Request.JSON_KEY_ERRORS).getJSONObject(0);
					
					String errorCode = error.getString(Annotator.JSON_KEY_CODE);
					String errorText = error.getString(Annotator.JSON_KEY_TEXT);
					
					throw new RequestErrorException(errorCode, errorText);
				}
				catch(JSONException e) {
					throw new IllegalArgumentException(
							"The failed JSON response doesn't contain a proper error object.",
							e);
				}
			}
		}
		catch(JSONException e) {
			// The result is JSON, but it isn't ohmage JSON, so we can safely
			// abort.
		}
	}
	
	/**
	 * Parses the byte array returned from the 
	 * {@link #makeRequest(URL, Map, boolean)} call, verifies that it is JSON,
	 * ensures that it was a successful call, and returns the string value
	 * associated with the given key.
	 * 
	 * @param response The byte array response as returned by 
	 * 				   {@link #makeRequest(URL, Map, boolean)}.
	 * 
	 * @param jsonKey The key string to use to get the response value from the
	 * 				  successful JSON response. If null, the response will be
	 * 				  checked for success or error and, if error, will throw an
	 * 				  exception.
	 * 
	 * @return Returns the value associated with the 'jsonKey'.
	 * 
	 * @throws IllegalArgumentException Thrown if the response cannot be 
	 * 									decoded into JSON or if there is no
	 * 									such key with the responded JSON.
	 * 
	 * @throws RequestErrorException Thrown if the server returned a valid JSON
	 * 								 response, but the request failed.
	 * 
	 * @see #makeRequest(URL, Map, boolean)
	 */
	private String processJsonResponse(final byte[] response, 
			final String jsonKey) throws RequestErrorException {
		
		JSONObject jsonResponse;
		try {
			jsonResponse = new JSONObject(new String(response));
		}
		catch(JSONException e) {
			throw new IllegalArgumentException(
					"The response is not valid JSON.", 
					e);
		}
		
		boolean success;
		try {
			success = Request.RESULT_SUCCESS.equals(
					jsonResponse.getString(Request.JSON_KEY_RESULT));
		}
		catch(JSONException e) {
			throw new IllegalArgumentException(
					"There is no '" + 
						Request.RESULT_SUCCESS + 
						"' key in the response, but it is valid JSON. " +
						"This indicates a deeper problem with the server.", 
					e);
		}
		
		if((success) && (jsonKey != null)) {
			try {
				return jsonResponse.getString(jsonKey);
			}
			catch(JSONException e) {
				throw new IllegalArgumentException(
						"The key '" + jsonKey + "' does not exist in the successful JSON response.", e);
			}
		}
		else if(success) {
			return null;
		}
		else {
			// Generate an ohmage-specific error.
			try {
				JSONObject error = jsonResponse.getJSONArray(
						Request.JSON_KEY_ERRORS).getJSONObject(0);
				
				String errorCode = error.getString(Annotator.JSON_KEY_CODE);
				String errorText = error.getString(Annotator.JSON_KEY_TEXT);
				
				throw new RequestErrorException(errorCode, errorText);
			}
			catch(JSONException e) {
				throw new IllegalArgumentException(
						"The failed JSON response doesn't contain a proper error object.",
						e);
			}
		}
	}
}