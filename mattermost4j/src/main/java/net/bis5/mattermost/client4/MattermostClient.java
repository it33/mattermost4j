/*
 * @(#) net.bis5.mattermost.client4.MattermostClient
 * Copyright (c) 2017 Maruyama Takayuki <bis5.wsys@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 */
package net.bis5.mattermost.client4;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.rx.java8.RxCompletionStage;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import net.bis5.mattermost.client4.model.AddChannelMemberRequest;
import net.bis5.mattermost.client4.model.AttachDeviceIdRequest;
import net.bis5.mattermost.client4.model.CheckUserMfaRequest;
import net.bis5.mattermost.client4.model.DeauthorizeOAuthAppRequest;
import net.bis5.mattermost.client4.model.LoginRequest;
import net.bis5.mattermost.client4.model.ResetPasswordRequest;
import net.bis5.mattermost.client4.model.RevokeSessionRequest;
import net.bis5.mattermost.client4.model.SearchPostsRequest;
import net.bis5.mattermost.client4.model.SendPasswordResetEmailRequest;
import net.bis5.mattermost.client4.model.SendVerificationEmailRequest;
import net.bis5.mattermost.client4.model.UpdateRolesRequest;
import net.bis5.mattermost.client4.model.UpdateUserActiveRequest;
import net.bis5.mattermost.client4.model.UpdateUserMfaRequest;
import net.bis5.mattermost.client4.model.UpdateUserPasswordRequest;
import net.bis5.mattermost.client4.model.VerifyUserEmailRequest;
import net.bis5.mattermost.jersey.provider.MattermostModelMapperProvider;
import net.bis5.mattermost.model.Audits;
import net.bis5.mattermost.model.AuthorizeRequest;
import net.bis5.mattermost.model.Channel;
import net.bis5.mattermost.model.ChannelList;
import net.bis5.mattermost.model.ChannelMember;
import net.bis5.mattermost.model.ChannelMembers;
import net.bis5.mattermost.model.ChannelPatch;
import net.bis5.mattermost.model.ChannelSearch;
import net.bis5.mattermost.model.ChannelStats;
import net.bis5.mattermost.model.ChannelUnread;
import net.bis5.mattermost.model.ChannelView;
import net.bis5.mattermost.model.ClusterInfo;
import net.bis5.mattermost.model.Command;
import net.bis5.mattermost.model.CommandArgs;
import net.bis5.mattermost.model.CommandResponse;
import net.bis5.mattermost.model.Compliance;
import net.bis5.mattermost.model.Compliances;
import net.bis5.mattermost.model.Config;
import net.bis5.mattermost.model.Emoji;
import net.bis5.mattermost.model.IncomingWebhook;
import net.bis5.mattermost.model.OAuthApp;
import net.bis5.mattermost.model.OutgoingWebhook;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.PostList;
import net.bis5.mattermost.model.PostPatch;
import net.bis5.mattermost.model.Preference;
import net.bis5.mattermost.model.Reaction;
import net.bis5.mattermost.model.Role;
import net.bis5.mattermost.model.SamlCertificateStatus;
import net.bis5.mattermost.model.Session;
import net.bis5.mattermost.model.Status;
import net.bis5.mattermost.model.SwitchRequest;
import net.bis5.mattermost.model.Team;
import net.bis5.mattermost.model.TeamMember;
import net.bis5.mattermost.model.TeamPatch;
import net.bis5.mattermost.model.TeamSearch;
import net.bis5.mattermost.model.TeamStats;
import net.bis5.mattermost.model.TeamUnread;
import net.bis5.mattermost.model.User;
import net.bis5.mattermost.model.UserAutocomplete;
import net.bis5.mattermost.model.UserList;
import net.bis5.mattermost.model.UserPatch;
import net.bis5.mattermost.model.UserSearch;
import net.bis5.mattermost.model.WebrtcInfoResponse;
import net.bis5.mattermost.model.license.MfaSecret;

/**
 * TODO 型の説明
 * 
 * @author Maruyama Takayuki
 * @since 2017/06/10
 */
public class MattermostClient {

	protected static final String API_URL_SUFFIX = "/api/v4";
	private final String url;
	private final String apiUrl;
	private String authToken;
	private AuthType authType;
	private final Client httpClient = buildClient();

	protected Client buildClient() {
		return ClientBuilder.newBuilder()
				.register(MattermostModelMapperProvider.class)
				.register(JacksonFeature.class)
				.register(
						new LoggingFeature(Logger.getLogger(getClass().getName()), Level.SEVERE, Verbosity.PAYLOAD_ANY,
								1000))
				.register(MultiPartFeature.class)
				.build();
	}

	public MattermostClient(String url) {
		this.url = url;
		this.apiUrl = url + API_URL_SUFFIX;
	}

	public void setOAuthToken(String token) {
		this.authToken = token;
		this.authType = AuthType.TOKEN;
	}

	public void clearOAuthToken() {
		this.authToken = null;
		this.authType = AuthType.BEARER;
	}

	public String getUsersRoute() {
		return "/users";
	}

	public String getUserRoute(String userId) {
		return getUsersRoute() + String.format("/%s", StringUtils.stripToEmpty(userId));
	}

	public String getUserByUsernameRoute(String userName) {
		return getUsersRoute() + String.format("/username/%s", StringUtils.stripToEmpty(userName));
	}

	public String getUserByEmailRoute(String email) {
		return getUsersRoute() + String.format("/email/%s", StringUtils.stripToEmpty(email));
	}

	public String getTeamsRoute() {
		return "/teams";
	}

	public String getTeamRoute(String teamId) {
		return getTeamsRoute() + String.format("/%s", StringUtils.stripToEmpty(teamId));
	}

	public String getTeamAutoCompleteCommandsRoute(String teamId) {
		return getTeamsRoute() + String.format("/%s/commands/autocomplete", StringUtils.stripToEmpty(teamId));
	}

	public String getTeamByNameRoute(String teamName) {
		return getTeamsRoute() + String.format("/name/%s", StringUtils.stripToEmpty(teamName));
	}

	public String getTeamMemberRoute(String teamId, String userId) {
		return getTeamRoute(teamId) + String.format("/members/%s", StringUtils.stripToEmpty(userId));
	}

	public String getTeamMembersRoute(String teamId) {
		return getTeamRoute(teamId) + "/members";
	}

	public String getTeamStatsRoute(String teamId) {
		return getTeamRoute(teamId) + "/stats";
	}

	public String getTeamImportRoute(String teamId) {
		return getTeamRoute(teamId) + "/import";
	}

	public String getChannelsRoute() {
		return "/channels";
	}

	public String getChannelsForTeamRoute(String teamId) {
		return getTeamRoute(teamId) + "/channels";
	}

	public String getChannelRoute(String channelId) {
		return getChannelsRoute() + String.format("/%s", StringUtils.stripToEmpty(channelId));
	}

	public String getChannelByNameRoute(String channelName, String teamId) {
		return getTeamRoute(teamId) + String.format("/channels/name/%s", StringUtils.stripToEmpty(channelName));
	}

	public String getChannelByNameForTeamNameRoute(String channelName, String teamName) {
		return getTeamByNameRoute(teamName) + String.format("/channels/name/%s", StringUtils.stripToEmpty(channelName));
	}

	public String getChannelMembersRoute(String channelId) {
		return getChannelRoute(channelId) + "/members";
	}

	public String getChannelMemberRoute(String channelId, String userId) {
		return getChannelMembersRoute(channelId) + String.format("/%s", StringUtils.stripToEmpty(userId));
	}

	public String getPostsRoute() {
		return "/posts";
	}

	public String getConfigRoute() {
		return "/config";
	}

	public String getLicenseRoute() {
		return "/license";
	}

	public String getPostRoute(String postId) {
		return getPostsRoute() + String.format("%s.", StringUtils.stripToEmpty(postId));
	}

	public String getFilesRoute() {
		return "/files";
	}

	public String getFileRoute(String fileId) {
		return getFilesRoute() + String.format("/%s", StringUtils.stripToEmpty(fileId));
	}

	public String getSystemRoute() {
		return "/system";
	}

	public String getTestEmailRoute() {
		return "/email/test";
	}

	public String getDatabaseRoute() {
		return "/database";
	}

	public String getCacheRoute() {
		return "/cache";
	}

	public String getClusterRoute() {
		return "/cluster";
	}

	public String getIncomingWebhooksRoute() {
		return "/hooks/incoming";
	}

	public String getIncomingWebhookRoute(String hookId) {
		return getIncomingWebhooksRoute() + String.format("/%s", StringUtils.stripToEmpty(hookId));
	}

	public String getComplianceReportsRoute() {
		return "?compliance/reports";
	}

	public String getComplianceReportRoute(String reportId) {
		return String.format("/compliance/reports/%s", StringUtils.stripToEmpty(reportId));
	}

	public String getOutgoingWebhooksRoute() {
		return "/hooks/outgoing";
	}

	public String getOutgoingWebhookRoute(String hookId) {
		return getOutgoingWebhooksRoute() + String.format("/%s", StringUtils.stripToEmpty(hookId));
	}

	public String getPreferencesRoute(String userId) {
		return getUserRoute(userId) + "/preferences";
	}

	public String getUserStatusRoute(String userId) {
		return getUserRoute(userId) + "/status";
	}

	public String getUserStatusesRoute() {
		return getUsersRoute() + "/status";
	}

	public String getSamlRoute() {
		return "/saml";
	}

	public String getLdapRoute() {
		return "/ldap";
	}

	public String getBrandRoute() {
		return "/brand";
	}

	public String getCommandsRoute() {
		return "/commands";
	}

	public String getCommandRoute(String commandId) {
		return getCommandsRoute() + String.format("/%s", StringUtils.stripToEmpty(commandId));
	}

	public String getEmojisRoute() {
		return "/emoji";
	}

	public String getEmojiRoute(String emojiId) {
		return getEmojisRoute() + String.format("/%s", StringUtils.stripToEmpty(emojiId));
	}

	public String getReactionsRoute() {
		return "/reactions";
	}

	public String getOAuthAppsRoute() {
		return "/oauth/apps";
	}

	public String getOAuthAppRoute(String appId) {
		return String.format("/oauth/apps/%s", StringUtils.stripToEmpty(appId));
	}

	protected <T> CompletionStage<ApiResponse<T>> doApiGet(String url, String etag, Class<T> responseType) {
		return doApiRequest(HttpMethod.GET, apiUrl + url, null, etag, responseType);
	}

	protected <T> CompletionStage<ApiResponse<T>> doApiGet(String url, String etag, GenericType<T> responseType) {
		return doApiRequest(HttpMethod.GET, apiUrl + url, null, etag, responseType);
	}

	protected CompletionStage<ApiResponse<Void>> doApiGet(String url, String etag) {
		return doApiRequest(HttpMethod.GET, apiUrl + url, null, etag);
	}

	protected <T, U> CompletionStage<ApiResponse<T>> doApiPost(String url, U data, Class<T> responseType) {
		return doApiRequest(HttpMethod.POST, apiUrl + url, data, null, responseType);
	}

	protected <T, U> CompletionStage<ApiResponse<T>> doApiPost(String url, U data, GenericType<T> responseType) {
		return doApiRequest(HttpMethod.POST, apiUrl + url, data, null, responseType);
	}

	protected <U> CompletionStage<ApiResponse<Void>> doApiPost(String url, U data) {
		return doApiRequest(HttpMethod.POST, apiUrl + url, data, null);
	}

	protected <T> CompletionStage<ApiResponse<Void>> doApiPostMultiPart(String url, MultiPart multiPart) {
		return RxCompletionStage.from(httpClient.target(url))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header(HEADER_AUTH, getAuthority())
				.rx()
				.method(HttpMethod.POST, Entity.entity(multiPart, multiPart.getMediaType()))
				.thenApply(r -> ApiResponse.of(r, Void.class));
	}

	protected <T, U> CompletionStage<ApiResponse<T>> doApiPut(String url, U data, Class<T> responseType) {
		return doApiRequest(HttpMethod.PUT, apiUrl + url, data, null, responseType);
	}

	protected <U> CompletionStage<ApiResponse<Void>> doApiPut(String url, U data) {
		return doApiRequest(HttpMethod.PUT, apiUrl + url, data, null);
	}

	protected <T> CompletionStage<ApiResponse<T>> doApiDelete(String url, Class<T> responseType) {
		return doApiRequest(HttpMethod.DELETE, apiUrl + url, null, null, responseType);
	}

	protected CompletionStage<ApiResponse<Void>> doApiDelete(String url) {
		return doApiRequest(HttpMethod.DELETE, apiUrl + url, null, null);
	}

	protected <T, U> CompletionStage<ApiResponse<T>> doApiRequest(String method, String url, U data, String etag,
			Class<T> responseType) {
		return RxCompletionStage.from(httpClient.target(url))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header(HEADER_ETAG_CLIENT, etag)
				.header(HEADER_AUTH, getAuthority())
				.rx()
				.method(method, Entity.json(data))
				.thenApply(r -> ApiResponse.of(r, responseType));
	}

	protected <T, U> CompletionStage<ApiResponse<T>> doApiRequest(String method, String url, U data, String etag,
			GenericType<T> responseType) {
		return RxCompletionStage.from(httpClient.target(url))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header(HEADER_ETAG_CLIENT, etag)
				.header(HEADER_AUTH, getAuthority())
				.rx()
				.method(method, Entity.json(data))
				.thenApply(r -> ApiResponse.of(r, responseType));
	}

	protected <U> CompletionStage<ApiResponse<Void>> doApiRequest(String method, String url, U data,
			String etag) {
		return RxCompletionStage.from(httpClient.target(url))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header(HEADER_ETAG_CLIENT, etag)
				.header(HEADER_AUTH, getAuthority())
				.rx()
				.method(method, Entity.json(data))
				.thenApply(r -> ApiResponse.of(r, Void.class));
	}

	private String getAuthority() {
		return authToken != null ? authType.getCode() + " " + authToken : null;
	}

	protected static final String HEADER_ETAG_CLIENT = "If-None-Match";
	protected static final String HEADER_AUTH = "Authorization";

	// TODO upload api

	/**
	 * a convenience function for checking the standard OK response from the web
	 * service.
	 * 
	 * @param apiResponse
	 * @return
	 */
	protected ApiResponse<Boolean> checkStatusOK(ApiResponse<Void> apiResponse) {
		Response response = apiResponse.getRawResponse();
		response.bufferEntity();
		Map<String, String> m = response.readEntity(stringMapType());
		if (m != null && m.getOrDefault(STATUS, "").equalsIgnoreCase(STATUS_OK)) {
			return ApiResponse.of(response, true);
		}
		return ApiResponse.of(response, false);
	}

	protected static final String STATUS = "status";
	protected static final String STATUS_OK = "ok";

	// Authentication Section

	/**
	 * authenticates a user by user id and password.
	 * 
	 * @param id
	 * @param password
	 * @return
	 */
	public CompletionStage<User> loginById(String id, String password) {
		return login(LoginRequest.builder().id(id).password(password).build());
	}

	/**
	 * authenticates a user by login id, which can be username, email, or some
	 * sort of SSO identifier based on server configuration, and a password.
	 * 
	 * @param loginId
	 * @param password
	 * @return
	 */
	public CompletionStage<User> login(String loginId, String password) {
		return login(LoginRequest.builder().loginId(loginId).password(password).build());
	}

	/**
	 * authenticates a user by LDAP id and password.
	 * 
	 * @param loginId
	 * @param password
	 * @return
	 */
	public CompletionStage<User> loginByLdap(String loginId, String password) {
		return login(LoginRequest.builder().loginId(loginId).password(password).ldapOnly(true).build());
	}

	/**
	 * authenticates a user by login id (username, email or some sort of SSO
	 * identifier based on configuration), password and attaches a device id to
	 * the session.
	 * 
	 * @param loginId
	 * @param password
	 * @param deviceId
	 * @return
	 */
	public CompletionStage<User> loginWithDevice(String loginId, String password, String deviceId) {
		return login(LoginRequest.builder().loginId(loginId).password(password).deviceId(deviceId).build());
	}

	protected CompletionStage<User> login(LoginRequest param) {
		return doApiPost("/users/login", param)
				.thenApply(this::onLogin)
				.thenApply(r -> r.readEntity());
	}

	protected ApiResponse<User> onLogin(ApiResponse<Void> loginResponse) {
		authToken = loginResponse.getRawResponse().getHeaderString(HEADER_TOKEN);
		authType = AuthType.BEARER;
		return ApiResponse.of(loginResponse.getRawResponse(), User.class);
	}

	private static final String HEADER_TOKEN = "token";

	/**
	 * terminates the current user's session.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> logout() {
		return doApiPost("/users/logout", "").thenApply(this::onLogout);
	}

	protected ApiResponse<Boolean> onLogout(ApiResponse<Void> logoutResponse) {
		authToken = null;
		authType = AuthType.BEARER;

		return checkStatusOK(logoutResponse);
	}

	/**
	 * changes a user's login type from one type to another.
	 * 
	 * @return
	 */
	public CompletionStage<String> switchAccountType(SwitchRequest switchRequest) {
		return doApiPost(getUsersRoute() + "/login/switch", switchRequest, stringMapType())
				.thenApply(r -> r.readEntity())
				.thenApply(m -> m.get("follow_link"));
	}

	// User Section

	/**
	 * creates a user in the system based on the provided user object.
	 * 
	 * @param user
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> createUser(User user) {
		return doApiPost(getUsersRoute(), user, User.class);
	}

	/**
	 * returns the logged in user.
	 * 
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> getMe(String etag) {
		return doApiGet(getUserRoute(ME), etag, User.class);
	}

	private static final String ME = "me";

	/**
	 * returns a user based on the provided user id string.
	 * 
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> getUser(String userId, String etag) {
		return doApiGet(getUserRoute(userId), etag, User.class);
	}

	/**
	 * returns a user based pn the provided user name string.
	 * 
	 * @param userName
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> getUserByUsername(String userName, String etag) {
		return doApiGet(getUserByUsernameRoute(userName), etag, User.class);
	}

	/**
	 * returns a user based on the provided user email string.
	 * 
	 * @param email
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> getUserByEmail(String email, String etag) {
		return doApiGet(getUserByEmailRoute(email), etag, User.class);
	}

	/**
	 * returns the users on a team based on search term.
	 * 
	 * @param teamId
	 * @param username
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserAutocomplete>> autocompleteUsersInTeam(String teamId, String username,
			String etag) {
		String query = String.format("?in_team=%s&name=%s", teamId, username);
		return doApiGet(getUsersRoute() + "/autocomplete" + query, etag, UserAutocomplete.class);
	}

	/**
	 * returns the users in a channel based on search term.
	 * 
	 * @param teamId
	 * @param channelId
	 * @param username
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserAutocomplete>> autocompleteUsersInChannel(String teamId, String channelId,
			String username, String etag) {
		String query = String.format("?in_team=%s&in_channel=%s&name=%s", teamId, channelId, username);
		return doApiGet(getUsersRoute() + "/autocomplete" + query, etag, UserAutocomplete.class);
	}

	/**
	 * returns the users in the system based on search term.
	 * 
	 * @param username
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserAutocomplete>> autocompleteUsers(String username, String etag) {
		String query = String.format("?name=%s", username);
		return doApiGet(getUsersRoute() + "/autocomplete" + query, etag, UserAutocomplete.class);
	}

	/**
	 * gets user's profile image. Must be logged in or be a system
	 * administrator.
	 * 
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<byte[]>> getProfileImage(String userId, String etag) {
		// XXX byte[]で返すの微妙・・・というかreadEntityでこけない?
		return doApiGet(getUserRoute(userId) + "/image", etag, byte[].class);
	}

	private GenericType<Map<String, String>> stringMapType() {
		return new GenericType<Map<String, String>>() {
		};
	}

	private <T> GenericType<List<T>> listType() {
		return new GenericType<List<T>>() {
		};
	}

	/**
	 * returns a page of users on the system. Page counting starts at 0.
	 * 
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsers(int page, int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getUsersRoute() + query, etag, UserList.class);
	}

	/**
	 * returns a page of users on a team. Page counting starts at 0.
	 * 
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersInTeam(String teamId, int page, int perPage, String etag) {
		String query = String.format("?in_team=%s&page=%d&per_page=%d", teamId, page, perPage);
		return doApiGet(getUsersRoute() + query, etag, UserList.class);
	}

	/**
	 * returns a page of users who are not in a team. Page counting starts at 0.
	 * 
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersNotInTeam(String teamId, int page, int perPage,
			String etag) {
		String query = String.format("?not_in_team=%s&page=%d&per_page=%d", teamId, page, perPage);
		return doApiGet(getUsersRoute() + query, etag, UserList.class);
	}

	/**
	 * returns a page of users on a team. Page counting starts at 0.
	 * 
	 * @param channelId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersInChannel(String channelId, int page, int perPage,
			String etag) {
		String query = String.format("?in_channel=%s&page=%d&per_page=%d", channelId, page, perPage);
		return doApiGet(getUsersRoute() + query, etag, UserList.class);
	}

	/**
	 * returns a page of users on a team. Page counting starts at 0.
	 * 
	 * @param teamId
	 * @param channelId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersNotInChannel(String teamId, String channelId, int page,
			int perPage,
			String etag) {
		String query = String.format("?in_team=%s&not_in_channel=%s&page=%d&per_page=%d", teamId, channelId, page,
				perPage);
		return doApiGet(getUsersRoute() + query, etag, UserList.class);
	}

	/**
	 * returns a page of users on the system that aren't on any teams. Page
	 * counting starts at 0.
	 * 
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersWithoutTeam(int page, int perPage, String etag) {
		String query = String.format("?without_team=1&page=%d&per_page=%d", page, perPage);
		return doApiGet(getUsersRoute() + query, etag, UserList.class);
	}

	/**
	 * returns a list of users based on the provided user ids.
	 * 
	 * @param userIds
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersByIds(String... userIds) {
		return doApiPost(getUsersRoute() + "/ids", userIds, UserList.class);
	}

	/**
	 * returns a list of users based on the provided usernames.
	 * 
	 * @param usernames
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> getUsersByUsernames(String... usernames) {
		return doApiPost(getUsersRoute() + "/usernames", usernames, UserList.class);
	}

	/**
	 * returns a list of users based on some search criteria.
	 * 
	 * @param search
	 * @return
	 */
	public CompletionStage<ApiResponse<UserList>> searchUsers(UserSearch search) {
		return doApiPost(getUsersRoute() + "/search", search, UserList.class);
	}

	/**
	 * updates a user in the system based on the provided user object.
	 * 
	 * @param user
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> updateUser(User user) {
		return doApiPut(getUserRoute(user.getId()), user, User.class);
	}

	/**
	 * partially updates a user in the system. Any missing fields are not
	 * updated.
	 * 
	 * @param userId
	 * @param patch
	 * @return
	 */
	public CompletionStage<ApiResponse<User>> patchUser(String userId, UserPatch patch) {
		return doApiPut(getUserRoute(userId) + "/patch", patch, User.class);
	}

	/**
	 * activates multi-factor authentication for a user if activate is true and
	 * a valid code is provided. If activate is false, then code is not required
	 * and multi-factor authentication is disabled for the user.
	 * 
	 * @param userId
	 * @param code
	 * @param activate
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateUserMfa(String userId, String code, boolean activate) {
		UpdateUserMfaRequest request = UpdateUserMfaRequest.builder().activate(activate).code(code).build();
		return doApiPut(getUserRoute(userId) + "/mfa", request).thenApply(this::checkStatusOK);
	}

	/**
	 * checks whether a user has MFA active on their account or not based on the
	 * provided login id.
	 * 
	 * @param loginId
	 * @return
	 */
	public CompletionStage<Boolean> checkUserMfa(String loginId) {
		CheckUserMfaRequest request = CheckUserMfaRequest.builder().loginId(loginId).build();
		return doApiPost(getUsersRoute() + "/mfa", request, stringMapType())
				.thenApply(r -> r.readEntity())
				.thenApply(m -> m.getOrDefault("mfa_required", "false"))
				.thenApply(Boolean::valueOf);
	}

	/**
	 * will generate a new MFA secret for a user and return it as a string and
	 * as a base64 encoded image QR code.
	 * 
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<MfaSecret>> generateMfaSecret(String userId) {
		return doApiPost(getUserRoute(userId) + "/mfa/generate", null, MfaSecret.class);
	}

	/**
	 * updates a user's password. Must be logged in as the user or be a system
	 * administrator.
	 * 
	 * @param userId
	 * @param currentPassword
	 * @param newPassword
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateUserPassword(String userId, String currentPassword,
			String newPassword) {
		UpdateUserPasswordRequest request = UpdateUserPasswordRequest.builder().currentPassword(currentPassword)
				.newPassword(newPassword).build();
		return doApiPut(getUserRoute(userId) + "/password", request).thenApply(this::checkStatusOK);
	}

	/**
	 * updates a user's roles in the system. A user can have "system_user" and
	 * "system_admin" roles.
	 * 
	 * @param userId
	 * @param role
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateUserRoles(String userId, Role... roles) {
		UpdateRolesRequest request = new UpdateRolesRequest(roles);
		return doApiPut(getUserRoute(userId) + "/roles", request).thenApply(this::checkStatusOK);
	}

	/**
	 * updates status of a user whether active or not.
	 * 
	 * @param userId
	 * @param active
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateUserActive(String userId, boolean active) {
		UpdateUserActiveRequest request = UpdateUserActiveRequest.builder().active(active).build();
		return doApiPut(getUserRoute(userId) + "/active", request).thenApply(this::checkStatusOK);
	}

	/**
	 * deactivates a user in the system based on the provided user id string.
	 * 
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteUser(String userId) {
		return doApiDelete(getUserRoute(userId)).thenApply(this::checkStatusOK);
	}

	/**
	 * will send a link for password resetting to a user with the provided
	 * email.
	 * 
	 * @param email
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> sendPasswordResetEmail(String email) {
		SendPasswordResetEmailRequest request = SendPasswordResetEmailRequest.builder().email(email).build();
		return doApiPost(getUsersRoute() + "/password/reset/send", request).thenApply(this::checkStatusOK);
	}

	/**
	 * uses a recovery code to update reset a user's password.
	 * 
	 * @param token
	 * @param newPassword
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> resetPassword(String token, String newPassword) {
		ResetPasswordRequest request = ResetPasswordRequest.builder().token(token).newPassword(newPassword).build();
		return doApiPost(getUsersRoute() + "/password/reset", request).thenApply(this::checkStatusOK);
	}

	/**
	 * returns a list of sessions based on the provided user id string.
	 * 
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Session>>> getSessions(String userId, String etag) {
		return doApiGet(getUserRoute(userId) + "/sessions", etag, listType());
	}

	/**
	 * revokes a user session based on the provided user id and session id
	 * strings.
	 * 
	 * @param userId
	 * @param sessionId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> revokeSession(String userId, String sessionId) {
		RevokeSessionRequest request = RevokeSessionRequest.builder().sessionId(sessionId).build();
		return doApiPost(getUserRoute(userId) + "/sessions/revoke", request).thenApply(this::checkStatusOK);
	}

	/**
	 * attaches a mobile device ID to the current session/
	 * 
	 * @param deviceId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> attachDeviceId(String deviceId) {
		AttachDeviceIdRequest request = AttachDeviceIdRequest.builder().deviceId(deviceId).build();
		return doApiPut(getUsersRoute() + "/sessions/device", request).thenApply(this::checkStatusOK);
	}

	/**
	 * will return a list with TeamUnread objects that contain the amount of
	 * unread messages and mentions the current user has for the teams it
	 * belongs to. An optional team ID can be set to exclude that team from the
	 * results. Must be authenticated.
	 * 
	 * @param userId
	 * @param teamIdToExclude
	 * @return
	 */
	public CompletionStage<ApiResponse<List<TeamUnread>>> getTeamsUnreadForUser(String userId, String teamIdToExclude) {
		String optional = "";
		if (teamIdToExclude != null) { // TODO use StringUtils.isNotEmpty
			try {
				optional = String.format("?exclude_team=%s",
						URLEncoder.encode(teamIdToExclude, StandardCharsets.UTF_8.displayName()));
			} catch (UnsupportedEncodingException e) {
				throw new AssertionError(e);
			}
		}
		return doApiGet(getUserRoute(userId) + "/teams/unread" + optional, null, listType());
	}

	/**
	 * returns a list of audit based on the provided user id string.
	 * 
	 * @param userId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Audits>> getUserAudits(String userId, int page, int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getUserRoute(userId) + "/audits" + query, etag, Audits.class);
	}

	/**
	 * will verify a user's email using the supplied token.
	 * 
	 * @param token
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> verifyUserEmail(String token) {
		VerifyUserEmailRequest request = VerifyUserEmailRequest.builder().token(token).build();
		return doApiPost(getUsersRoute() + "/email/verify", request).thenApply(this::checkStatusOK);
	}

	/**
	 * will send an email to the user with the provided email addresses, if that
	 * user exists. The email will contain a link that can be used to verify the
	 * user's email address.
	 * 
	 * @param email
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> sendVerificationEmail(String email) {
		SendVerificationEmailRequest request = SendVerificationEmailRequest.builder().email(email).build();
		return doApiPost(getUsersRoute() + "/email/verify/send", request).thenApply(this::checkStatusOK);
	}

	/**
	 * sets profile image of the user.
	 * 
	 * @param userId
	 * @param imageFilePath
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> setProfileImage(String userId, Path imageFilePath) {
		MultiPart multiPart = new MultiPart();
		multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

		FileDataBodyPart body = new FileDataBodyPart("image", imageFilePath.toFile());
		multiPart.bodyPart(body);

		return doApiPostMultiPart(getUserRoute(userId) + "/image", multiPart).thenApply(this::checkStatusOK);
	}

	// Team Section

	/**
	 * creates a team in the system based on the provided team object.
	 * 
	 * @param team
	 * @return
	 */
	public CompletionStage<ApiResponse<Team>> createTeam(Team team) {
		return doApiPost(getTeamsRoute(), team, Team.class);
	}

	/**
	 * returns a team based on the provided team is string/
	 * 
	 * @param teamId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Team>> getTeam(String teamId, String etag) {
		return doApiGet(getTeamRoute(teamId), etag, Team.class);
	}

	/**
	 * returns all teams based on permssions.
	 * 
	 * @param etag
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Team>>> getAllTeams(String etag, int page, int perPage) {
		String query = String.format("?page=%s&per_page=%d", page, perPage);
		return doApiGet(getTeamsRoute() + query, etag, listType());
	}

	/**
	 * returns a team based on the provided team name string.
	 * 
	 * @param name
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Team>> getTeamByName(String name, String etag) {
		return doApiGet(getTeamByNameRoute(name), etag, Team.class);
	}

	/**
	 * returns teams matching the provided search term.
	 * 
	 * @param search
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Team>>> searchTeams(TeamSearch search) {
		return doApiPost(getTeamsRoute() + "/search", search, listType());
	}

	/**
	 * returns true or false if the team exist or not.
	 * 
	 * @param name
	 * @param etag
	 * @return
	 */
	public CompletionStage<Boolean> teamExists(String name, String etag) {
		return doApiGet(getTeamByNameRoute(name) + "/exists", etag, stringMapType())
				.thenApply(r -> r.readEntity())
				.thenApply(m -> m.getOrDefault("exists", "false"))
				.thenApply(Boolean::valueOf);
	}

	/**
	 * returns a list of teams a user is on. Must be logged in as the user or be
	 * a system administrator.
	 * 
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Team>>> getTeamsForUser(String userId, String etag) {
		return doApiGet(getUserRoute(userId) + "/teams", etag, listType());
	}

	/**
	 * returns a team member based on the provided team and user id strings.
	 * 
	 * @param teamId
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<TeamMember>> getTeamMember(String teamId, String userId, String etag) {
		return doApiGet(getTeamMemberRoute(teamId, userId), etag, TeamMember.class);
	}

	/**
	 * will update the roles on a team for a user.
	 * 
	 * @param teamId
	 * @param userId
	 * @param newRoles
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateTeamMemberRoles(String teamId, String userId, Role... newRoles) {
		UpdateRolesRequest request = new UpdateRolesRequest(newRoles);
		return doApiPut(getTeamMemberRoute(teamId, userId) + "/roles", request)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will update a team.
	 * 
	 * @param team
	 * @return
	 */
	public CompletionStage<ApiResponse<Team>> updateTeam(Team team) {
		return doApiPut(getTeamRoute(team.getId()), team, Team.class);
	}

	/**
	 * partially updates a team. Any missing fields are not updated.
	 * 
	 * @param teamId
	 * @param patch
	 * @return
	 */
	public CompletionStage<ApiResponse<Team>> patchTeam(String teamId, TeamPatch patch) {
		return doApiPut(getTeamRoute(teamId) + "/patch", patch, Team.class);
	}

	/**
	 * deletes the team softly (archive only, not permanent delete).
	 * 
	 * @param teamId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> softDeleteTeam(String teamId) {
		return doApiDelete(getTeamRoute(teamId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * returns team members based on the provided team id string.
	 * 
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<TeamMember>>> getTeamMembers(String teamId, int page, int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getTeamMembersRoute(teamId) + query, etag, listType());
	}

	/**
	 * returns the team member for a user.
	 * 
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<TeamMember>>> getTeamMembersForUser(String userId, String etag) {
		return doApiGet(getUserRoute(userId) + "/teams/members", etag, listType());
	}

	/**
	 * will return an array of team members based on the team id and a list of
	 * user ids provided. Must be authenticated.
	 * 
	 * @param teamId
	 * @param userIds
	 * @return
	 */
	public CompletionStage<ApiResponse<List<TeamMember>>> getTeamMembersByIds(String teamId, String... userIds) {
		String url = String.format("/teams/%s/members/ids", teamId);
		return doApiPost(url, userIds, listType());
	}

	/**
	 * adds user to a team and return a team member.
	 * 
	 * @param teamId
	 * @param userId
	 * @param hash
	 * @param dataToHash
	 * @param inviteId
	 * @return
	 */
	public CompletionStage<ApiResponse<TeamMember>> addTeamMember(String teamId, String userId, String hash,
			String dataToHash,
			String inviteId) {
		TeamMember member = new TeamMember(teamId, userId);

		String query = "";
		// FIXME inviteId != null && (hash != null && dataToHash != null) then
		// throw new IllegalArgumentException
		if (inviteId != null) { // FIXME StringUtils.isNotEmpty
			query += String.format("?invite_id=%s", inviteId);
		}
		if (hash != null && dataToHash != null) { // FIXME
													// StringUtils.isNotEmpty
			query += String.format("?hash=%s&data=%s", hash, dataToHash);
		}

		return doApiPost(getTeamMembersRoute(teamId) + query, member, TeamMember.class);
	}

	/**
	 * adds a number of users to a team and returns the team members.
	 * 
	 * @param teamId
	 * @param userIds
	 * @return
	 */
	public CompletionStage<ApiResponse<List<TeamMember>>> addTeamMembers(String teamId, String... userIds) {
		List<TeamMember> members = Arrays.stream(userIds)
				.map(u -> new TeamMember(teamId, u))
				.collect(Collectors.toList());

		return doApiPost(getTeamMembersRoute(teamId) + "/batch", members, listType());
	}

	/**
	 * will remove a user from a team.
	 * 
	 * @param teamId
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> removeTeamMember(String teamId, String userId) {
		return doApiDelete(getTeamMemberRoute(teamId, userId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * returns a team stats based on the team id string. Must be authenticated.
	 * 
	 * @param teamId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<TeamStats>> getTeamStats(String teamId, String etag) {
		return doApiGet(getTeamStatsRoute(teamId), etag, TeamStats.class);
	}

	/**
	 * will return a TeamUnread object that contains the amount of unread
	 * messages and mentions the user has for the specified team. Must be
	 * authenticated.
	 * 
	 * @param teamId
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<TeamUnread>> getTeamUnread(String teamId, String userId) {
		return doApiGet(getUserRoute(userId) + getTeamRoute(teamId) + "/unread", null, TeamUnread.class);
	}

	/**
	 * will import an exported team from other app into a existing team.
	 * 
	 * @param data
	 * @param filesize
	 * @param importFrom
	 * @param fileName
	 * @param teamId
	 * @return
	 */
	public CompletionStage<ApiResponse<byte[]>> importTeam(byte[] data, int filesize, String importFrom,
			String fileName,
			String teamId) {
		// FIXME
		throw new UnsupportedOperationException();
	}

	/**
	 * invite users by email to the team.
	 * 
	 * @param teamId
	 * @param userEmails
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> inviteUsersToTeam(String teamId, List<String> userEmails) {
		return doApiPost(getTeamRoute(teamId) + "/invite/email", userEmails)
				.thenApply(this::checkStatusOK);
	}

	// Channel Section

	/**
	 * creates a channel based on the provided channel object.
	 * 
	 * @param channel
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> createChannel(Channel channel) {
		return doApiPost(getChannelsRoute(), channel, Channel.class);
	}

	/**
	 * update a channel based on the provided channel object.
	 * 
	 * @param channel
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> updateChannel(Channel channel) {
		return doApiPut(getChannelRoute(channel.getId()), channel, Channel.class);
	}

	/**
	 * partially updates a channel. Any missing fields are not updated.
	 * 
	 * @param channelId
	 * @param patch
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> patchChannel(String channelId, ChannelPatch patch) {
		return doApiPut(getChannelRoute(channelId) + "/patch", patch, Channel.class);
	}

	/**
	 * creates a direct message channel based on the two user ids provided.
	 * 
	 * @param userId1
	 * @param userId2
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> createDirectChannel(String userId1, String userId2) {
		return doApiPost(getChannelsRoute() + "/direct", Arrays.asList(userId1, userId2), Channel.class);
	}

	/**
	 * creates a group message channel based on userIds provided.
	 * 
	 * @param userIds
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> createGroupChannel(String... userIds) {
		return doApiPost(getChannelsRoute() + "/group", userIds, Channel.class);
	}

	/**
	 * returns a channel based on the provided channel id string.
	 * 
	 * @param channelId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> getChannel(String channelId, String etag) {
		return doApiGet(getChannelRoute(channelId), etag, Channel.class);
	}

	/**
	 * returns statistics for a channel.
	 * 
	 * @param channelId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelStats>> getChannelStats(String channelId, String etag) {
		return doApiGet(getChannelRoute(channelId) + "/stats", etag, ChannelStats.class);
	}

	/**
	 * gets a list of pinned posts.
	 * 
	 * @param channelId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getPinnedPosts(String channelId, String etag) {
		return doApiGet(getChannelRoute(channelId) + "/pinned", etag, PostList.class);
	}

	/**
	 * returns a list of public channels based on the provided team id string.
	 * 
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelList>> getPublicChannelsForTeam(String teamId, int page, int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getChannelsForTeamRoute(teamId) + query, etag, ChannelList.class);
	}

	/**
	 * returns a list of public channeld based on provided team id string.
	 * 
	 * @param teamId
	 * @param channelIds
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelList>> getPublicChannelsByIdsForTeam(String teamId,
			String... channelIds) {
		return doApiPost(getChannelsForTeamRoute(teamId) + "/ids", channelIds, ChannelList.class);
	}

	/**
	 * returns a list channels of on a team for user.
	 * 
	 * @param teamId
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelList>> getChannelsForTeamForUser(String teamId, String userId,
			String etag) {
		return doApiGet(getUserRoute(userId) + getTeamRoute(teamId) + "/channels", etag, ChannelList.class);
	}

	/**
	 * returns the channels on a team matching the provided search term.
	 * 
	 * @param teamId
	 * @param search
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelList>> searchChannels(String teamId, ChannelSearch search) {
		return doApiPost(getChannelsForTeamRoute(teamId) + "/search", search, ChannelList.class);
	}

	/**
	 * deletes channel based on the provided channel id string.
	 * 
	 * @param channelId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteChannel(String channelId) {
		return doApiDelete(getChannelRoute(channelId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * returns a channel based on the provided channel name and team id strings.
	 * 
	 * @param channelName
	 * @param teamId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> getChannelByName(String channelName, String teamId, String etag) {
		return doApiGet(getChannelByNameRoute(channelName, teamId), etag, Channel.class);
	}

	/**
	 * returns a channel based on the provided channel name and team name
	 * strings.
	 * 
	 * @param channelName
	 * @param teamName
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Channel>> getChannelByNameForTeamName(String channelName, String teamName,
			String etag) {
		return doApiGet(getChannelByNameForTeamNameRoute(channelName, teamName), etag, Channel.class);
	}

	/**
	 * gets a page of channel members.
	 * 
	 * @param channelId
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelMembers>> getChannelMembers(String channelId, int page, int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getChannelMembersRoute(channelId) + query, etag, ChannelMembers.class);
	}

	/**
	 * gets the channel members in a channel for a list of user ids.
	 * 
	 * @param channelId
	 * @param userIds
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelMembers>> getChannelMembersByIds(String channelId, String... userIds) {
		return doApiPost(getChannelMembersRoute(channelId) + "/ids", userIds, ChannelMembers.class);
	}

	/**
	 * gets a channel memner.
	 * 
	 * @param channelId
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelMember>> getChannelMember(String channelId, String userId, String etag) {
		return doApiGet(getChannelMemberRoute(channelId, userId), etag, ChannelMember.class);
	}

	/**
	 * gets all the channel members for a user on a team.
	 * 
	 * @param userId
	 * @param teamId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelMembers>> getChannelMembersForUser(String userId, String teamId,
			String etag) {
		return doApiGet(getUserRoute(userId) + String.format("/teams/%s/channels/members", teamId), etag,
				ChannelMembers.class);
	}

	/**
	 * performs a view action for a user. synonymous with switching channels or
	 * marking channels as read by a user.
	 * 
	 * @param userId
	 * @param view
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> viewChannel(String userId, ChannelView view) {
		String url = String.format(getChannelsRoute() + "/members/%s/view", userId);
		return doApiPost(url, view).thenApply(this::checkStatusOK);
	}

	/**
	 * will return a ChannelUnread object that contains the number ofo unread
	 * messages and mentions for a user.
	 * 
	 * @param channelId
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelUnread>> getChannelUnread(String channelId, String userId) {
		return doApiGet(getUserRoute(userId) + getChannelRoute(channelId) + "/unread", null, ChannelUnread.class);
	}

	/**
	 * will update the roles on a channel for a user.
	 * 
	 * @param channelId
	 * @param userId
	 * @param roles
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateChannelRoles(String channelId, String userId, Role... roles) {
		UpdateRolesRequest request = new UpdateRolesRequest(roles);
		return doApiPut(getChannelMemberRoute(channelId, userId) + "/roles", request)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will update the notification properties on a channel for a user.
	 * 
	 * @param channelId
	 * @param userId
	 * @param props
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> updateChannelNotifyProps(String channelId, String userId,
			Map<String, String> props) {
		return doApiPut(getChannelMemberRoute(channelId, userId) + "/notify_props", props)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * adds user to channel and return a channel memner.
	 * 
	 * @param channelId
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<ChannelMember>> addChannelMember(String channelId, String userId) {
		AddChannelMemberRequest request = AddChannelMemberRequest.builder().userId(userId).build();
		return doApiPost(getChannelMembersRoute(channelId), request, ChannelMember.class);
	}

	/**
	 * will delete the channel member object for a user, effectively removing
	 * the user from a channel.
	 * 
	 * @param channelId
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> removeUserFromChannel(String channelId, String userId) {
		return doApiDelete(getChannelMemberRoute(channelId, userId))
				.thenApply(this::checkStatusOK);
	}

	// Post Section

	/**
	 * creates a post based on the provided post object.
	 * 
	 * @param post
	 * @return
	 */
	public CompletionStage<ApiResponse<Post>> createPost(Post post) {
		return doApiPost(getPostsRoute(), post, Post.class);
	}

	/**
	 * updates a post based on the provided post object.
	 * 
	 * @param postId
	 * @param post
	 * @return
	 */
	public CompletionStage<ApiResponse<Post>> updatePost(String postId, Post post) {
		return doApiPut(getPostRoute(postId), post, Post.class);
	}

	/**
	 * partially updates a post. Any missing fields are not updated.
	 * 
	 * @param postId
	 * @param patch
	 * @return
	 */
	public CompletionStage<ApiResponse<Post>> patchPost(String postId, PostPatch patch) {
		return doApiPut(getPostRoute(postId) + "/patch", patch, Post.class);
	}

	/**
	 * pin a post based on proviced post id string.
	 * 
	 * @param postId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> pinPost(String postId) {
		return doApiPost(getPostRoute(postId) + "/pin", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * unpin a post based on provided post id string.
	 * 
	 * @param postId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> unpinPost(String postId) {
		return doApiPost(getPostRoute(postId) + "/unpin", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * gets a single post.
	 * 
	 * @param postId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Post>> getPost(String postId, String etag) {
		return doApiGet(getPostRoute(postId), etag, Post.class);
	}

	/**
	 * deletes a post from the provided post id string.
	 * 
	 * @param postId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deletePost(String postId) {
		return doApiDelete(getPostRoute(postId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * gets a post with all the other posts in the same thread.
	 * 
	 * @param postId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getPostThread(String postId, String etag) {
		return doApiGet(getPostRoute(postId) + "/thread", etag, PostList.class);
	}

	/**
	 * gets a page of posts with an array for ordering for a channel.
	 * 
	 * @param channelId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getPostsForChannel(String channelId, int page, int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getChannelRoute(channelId) + "/posts" + query, etag, PostList.class);
	}

	/**
	 * returns flagges posts of a user based on user id string.
	 * 
	 * @param userId
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getFlaggedPostsForUser(String userId, int page, int perPage) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getUserRoute(userId) + "/posts/flagged" + query, null, PostList.class);
	}

	/**
	 * returns flagged posts in team of a user based on user id string.
	 * 
	 * @param userId
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getFlaggesPostsForUserInTeam(String userId, String teamId, int page,
			int perPage) {
		// TODO teamId length validation

		String query = String.format("?in_team=%s&page=%d&per_page=%d", teamId, page, perPage);
		return doApiGet(getUserRoute(userId) + "/posts/flagged" + query, null, PostList.class);
	}

	/**
	 * returns flagged posts in channel of a user based on user id string.
	 * 
	 * @param userId
	 * @param channelId
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getFlaggedPostsForUserInChannel(String userId, String channelId,
			int page,
			int perPage) {
		// TODO channelId length validation
		String query = String.format("?in_channel=%s&page=%d&per_page=%d", channelId, page, perPage);
		return doApiGet(getUserRoute(userId) + "/posts/flagged" + query, null, PostList.class);
	}

	/**
	 * gets posts created after a specified time as Unix time in milliseconds.
	 * 
	 * @param channelId
	 * @param time
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getPostsSince(String channelId, long time) {
		String query = String.format("?since=%d", time);
		return doApiGet(getChannelRoute(channelId) + "/posts" + query, null, PostList.class);
	}

	/**
	 * gets a page of posts that were posted after the post provided.
	 * 
	 * @param channelId
	 * @param postId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getPostsAfter(String channelId, String postId, int page, int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d&after=%s", page, perPage, postId);
		return doApiGet(getChannelRoute(channelId) + "/posts" + query, etag, PostList.class);
	}

	/**
	 * gets a page of posts that were posted before the post provided.
	 * 
	 * @param channelId
	 * @param postId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> getPostsBefore(String channelId, String postId, int page, int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d&before=%s", page, perPage, postId);
		return doApiGet(getChannelRoute(channelId) + "/posts" + query, etag, PostList.class);
	}

	/**
	 * returns any posts with matching term string.
	 * 
	 * @param teamId
	 * @param terms
	 * @param isOrSearch
	 * @return
	 */
	public CompletionStage<ApiResponse<PostList>> searchPosts(String teamId, String terms, boolean isOrSearch) {
		SearchPostsRequest request = SearchPostsRequest.builder().terms(terms).isOrSearch(isOrSearch).build();
		return doApiPost(getTeamRoute(teamId) + "/posts/search", request, PostList.class);
	}

	// TODO File Section

	// General Section

	/**
	 * will ping the server and to see if it is up and running.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> getPing() {
		return doApiGet(getSystemRoute() + "/ping", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will attempt to connect to the configured SMTP server.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> testEmail() {
		return doApiPost(getTestEmailRoute(), null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will retrieve the server config with some sanitized items.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Config>> getConfig() {
		return doApiGet(getConfigRoute(), null, Config.class);
	}

	/**
	 * will reload the server configuration.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> reloadConfig() {
		return doApiPost(getConfigRoute() + "/reload", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will retrieve the parts of the server configuration needed by the client,
	 * formatted in the old format.
	 * 
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Map<String, String>>> getOldClientConfig(String etag) {
		return doApiGet(getConfigRoute() + "/client?format=old", etag, stringMapType());
	}

	/**
	 * will retrieve the parts of the server license needed by the client,
	 * formatted in the old format.
	 * 
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Map<String, String>>> getOldClientLicense(String etag) {
		return doApiGet(getLicenseRoute() + "/client?format=old", etag, stringMapType());
	}

	/**
	 * will recycle the connections. Discard current connection and get new one.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> databaseRecycle() {
		return doApiPost(getDatabaseRoute() + "/recycle", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will purge the cache and can affect the performance while is cleaning.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> invalidateCaches() {
		return doApiPost(getCacheRoute() + "/invalidate", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will update the server configuration.
	 * 
	 * @param config
	 * @return
	 */
	public CompletionStage<ApiResponse<Config>> updateConfig(Config config) {
		return doApiPut(getConfigRoute(), config, Config.class);
	}

	// Webhooks Section

	/**
	 * creates an incoming webhook for a channel.
	 * 
	 * @param hook
	 * @return
	 */
	public CompletionStage<ApiResponse<IncomingWebhook>> createIncomingWebhook(IncomingWebhook hook) {
		return doApiPost(getIncomingWebhooksRoute(), hook, IncomingWebhook.class);
	}

	/**
	 * updates an incoming webhook for a channel.
	 * 
	 * @param hook
	 * @return
	 */
	public CompletionStage<ApiResponse<IncomingWebhook>> updateIncomingWebhook(IncomingWebhook hook) {
		return doApiPut(getIncomingWebhookRoute(hook.getId()), hook, IncomingWebhook.class);
	}

	/**
	 * returns a page of incoming webhooks on the system. Page counting starts
	 * at 0.
	 * 
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<IncomingWebhook>>> getIncomingWebhooks(int page, int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getIncomingWebhooksRoute() + query, etag, listType());
	}

	/**
	 * returns a page of incoming webhooks for a team. Page counting starts at
	 * 0.
	 * 
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<IncomingWebhook>>> getIncomingWebhooksForTeam(String teamId, int page,
			int perPage,
			String etag) {
		String query = String.format("?page=%d&per_page=%d&team_id=%s", page, perPage, teamId);
		return doApiGet(getIncomingWebhooksRoute() + query, etag, listType());
	}

	/**
	 * returns an Incoming webhook given the hook id.
	 * 
	 * @param hookId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<IncomingWebhook>> getIncomingWebhook(String hookId, String etag) {
		return doApiGet(getIncomingWebhookRoute(hookId), etag, IncomingWebhook.class);
	}

	/**
	 * deletes an Incoming Webhook given the hook id.
	 * 
	 * @param hookId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteIncomingWebhook(String hookId) {
		return doApiDelete(getIncomingWebhookRoute(hookId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * creates an outgoing webhook for a team or channel.
	 * 
	 * @param hook
	 * @return
	 */
	public CompletionStage<ApiResponse<OutgoingWebhook>> createOutgoingWebhook(OutgoingWebhook hook) {
		return doApiPost(getOutgoingWebhooksRoute(), hook, OutgoingWebhook.class);
	}

	/**
	 * updates an outgoing webhook.
	 * 
	 * @param hook
	 * @return
	 */
	public CompletionStage<ApiResponse<OutgoingWebhook>> updateOutgoingWebhook(OutgoingWebhook hook) {
		return doApiPut(getOutgoingWebhookRoute(hook.getId()), hook, OutgoingWebhook.class);
	}

	/**
	 * returns a page of outgoing webhooks ont eh system. Page counting starts
	 * at 0.
	 * 
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<OutgoingWebhook>>> getOutgoingWebhooks(int page, int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getOutgoingWebhooksRoute() + query, etag, listType());
	}

	/**
	 * outgoing webhooks on the system requested by hook id.
	 * 
	 * @param hookId
	 * @return
	 */
	public CompletionStage<ApiResponse<OutgoingWebhook>> getOutgoingWebhook(String hookId) {
		return doApiGet(getOutgoingWebhookRoute(hookId), null, OutgoingWebhook.class);
	}

	/**
	 * returns a page of outgoing webhooks for a channel. Page counting starts
	 * at 0.
	 * 
	 * @param channelId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<OutgoingWebhook>>> getOutgoingWebhooksForChannel(String channelId, int page,
			int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d&channel_id=%s", page, perPage, channelId);
		return doApiGet(getOutgoingWebhooksRoute() + query, etag, listType());
	}

	/**
	 * returns a page of outgoing webhooks for a team. Page counting starts at
	 * 0.
	 * 
	 * @param teamId
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<List<OutgoingWebhook>>> getOutgoingWebhooksForTeam(String teamId, int page,
			int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d&team_id=%s", page, perPage, teamId);
		return doApiGet(getOutgoingWebhooksRoute() + query, etag, listType());
	}

	/**
	 * regenerate the outgoing webhook token.
	 * 
	 * @param hookId
	 * @return
	 */
	public CompletionStage<ApiResponse<OutgoingWebhook>> regenOutgoingHookToken(String hookId) {
		return doApiPost(getOutgoingWebhookRoute(hookId) + "/regen_token", null, OutgoingWebhook.class);
	}

	/**
	 * delete the outgoing webhook on the system requested by hook id.
	 * 
	 * @param hookId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteOutgoingWebhook(String hookId) {
		return doApiDelete(getOutgoingWebhookRoute(hookId))
				.thenApply(this::checkStatusOK);
	}

	// Preferences Section

	/**
	 * returns the user's preferences.
	 * 
	 * @param userId
	 * @return
	 */
	public CompletionStage<ApiResponse<Preferences>> getPreferences(String userId) {
		return doApiGet(getPreferencesRoute(userId), null, Preferences.class);
	}

	/**
	 * saves the user's preferences.
	 * 
	 * @param userId
	 * @param preferences
	 * @return
	 */
	public CompletionStage<Boolean> updatePreferences(String userId, Preferences preferences) {
		return doApiPut(getPreferencesRoute(userId), preferences)
				.thenApply(r -> true);
	}

	/**
	 * deletes the user's preferences.
	 * 
	 * @param userId
	 * @param preferences
	 * @return
	 */
	public CompletionStage<Boolean> deletePreerences(String userId, Preferences preferences) {
		return doApiPost(getPreferencesRoute(userId) + "/delete", preferences)
				.thenApply(r -> true);
	}

	/**
	 * returns the user's preferences from the provided category string.
	 * 
	 * @param userId
	 * @param category
	 * @return
	 */
	public CompletionStage<ApiResponse<Preferences>> getPreferencesByCategory(String userId, String category) {
		String url = String.format(getPreferencesRoute(userId) + "/%s", category);
		return doApiGet(url, null, Preferences.class);
	}

	/**
	 * returns the user's preferences from the provided category and preference
	 * name string.
	 * 
	 * @param userId
	 * @param category
	 * @param preferenceName
	 * @return
	 */
	public CompletionStage<ApiResponse<Preference>> getPreferenceByCategoryAndName(String userId, String category,
			String preferenceName) {
		String url = String.format(getPreferencesRoute(userId) + "/%s/name/%s", category, preferenceName);
		return doApiGet(url, null, Preference.class);
	}

	/**
	 * returns metadata for the SAML configuration.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<String>> getSamlMetadata() {
		return doApiGet(getSamlRoute() + "/metadata", null, String.class);
	}

	protected Object samlFileToMultipart(Path dataFile, String dataFileName) {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	/**
	 * will upload an IDP certificate for SAML and set the config to use it.
	 * 
	 * @param dataFile
	 * @param fileName
	 * @return
	 */
	public CompletionStage<Boolean> uploadSamlIdpCertificate(Path dataFile, String fileName) {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	/**
	 * will upload a public certificate for SAML and set the config to use it.
	 * 
	 * @param dataFile
	 * @param fileName
	 * @return
	 */
	public CompletionStage<Boolean> uploadSamlPublicCertificate(Path dataFile, String fileName) {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	/**
	 * will upload a private key for SAML and set the config to use it.
	 * 
	 * @param dataFile
	 * @param fileName
	 * @return
	 */
	public CompletionStage<Boolean> uploadSamlPrivateCertificate(Path dataFile, String fileName) {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	/**
	 * deletes the SAML IDP certificate from the server and updates the config
	 * to not use it and disable SAML.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteSamlIdpCertificate() {
		return doApiDelete(getSamlRoute() + "/certificate/idp")
				.thenApply(this::checkStatusOK);
	}

	/**
	 * deletes the saml IDP certificate from the server and updates the config
	 * to not use it and disable SAML.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteSamlPublicCertificate() {
		return doApiDelete(getSamlRoute() + "/certificate/public")
				.thenApply(this::checkStatusOK);
	}

	/**
	 * deletes the SAML IDP certificate from the server and updates the config
	 * to not use it and disable SAML.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteSamlPrivateCertificate() {
		return doApiDelete(getSamlRoute() + "/certificate/private")
				.thenApply(this::checkStatusOK);
	}

	/**
	 * returns metadata for the SAML configuration.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<SamlCertificateStatus>> getSamlCertificateStatus() {
		return doApiGet(getSamlRoute() + "/certificate/status", null, SamlCertificateStatus.class);
	}

	// Compliance Section

	/**
	 * creates a compliance report.
	 * 
	 * @param report
	 * @return
	 */
	public CompletionStage<ApiResponse<Compliance>> createComplianceReport(Compliance report) {
		return doApiPost(getComplianceReportsRoute(), report, Compliance.class);
	}

	/**
	 * returns list of compliance reports.
	 * 
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<Compliances>> getComplianceReports(int page, int perPage) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getComplianceReportsRoute() + query, null, Compliances.class);
	}

	/**
	 * returns a compliance report.
	 * 
	 * @param reportId
	 * @return
	 */
	public CompletionStage<ApiResponse<Compliance>> getComplianceReport(String reportId) {
		return doApiGet(getComplianceReportRoute(reportId), null, Compliance.class);
	}

	/**
	 * returns a full compliance report as a file/
	 * 
	 * @param reportId
	 * @return
	 */
	public CompletionStage<ApiResponse<Object>> downloadComplianceReport(String reportId) {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	// Cluster Section

	/**
	 * @return
	 */
	public CompletionStage<ApiResponse<List<ClusterInfo>>> getClusterStatus() {
		return doApiGet(getClusterRoute() + "/status", null, listType());
	}

	// LDAP Section

	/**
	 * will force a sync with the configured LDAP server.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> syncLdap() {
		return doApiPost(getLdapRoute() + "/sync", null)
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will attempt to connect to the configured LDAP server and return OK if
	 * configured correctly.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> testLdap() {
		return doApiPost(getLdapRoute() + "/test", null)
				.thenApply(this::checkStatusOK);
	}

	// Audits Section

	/**
	 * returns a list of audits for the whole system.
	 * 
	 * @param page
	 * @param perPage
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Audits>> getAudits(int page, int perPage, String etag) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet("/audits" + query, etag, Audits.class);
	}

	// Brand Section

	/**
	 * retrieves the previously uploaded brand image.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<Object>> getBrandImage() {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	/**
	 * sets the brand image for the system.
	 * 
	 * @param dataFile
	 * @return
	 */
	public CompletionStage<Boolean> uploadBrandImage(Path dataFile) {
		throw new UnsupportedOperationException("not impl");// FIXME
	}

	// Logs Section

	/**
	 * page of logs as a string list.
	 * 
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<List<String>>> getLogs(int page, int perPage) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet("/logs" + query, null, listType());
	}

	/**
	 * This method is a convenience Web Service call so clients can log messages
	 * into the server-side logs. For example we typically log javascript error
	 * messages into the server-side. It returns the log message if the logging
	 * was successful.
	 * 
	 * @param message
	 * @return
	 */
	public CompletionStage<ApiResponse<Map<String, String>>> postLog(Map<String, String> message) {
		return doApiPost("/logs", message, stringMapType());
	}

	// OAuth Section

	/**
	 * will register a new OAuth 2.0 client application with Mattermost acting
	 * as an OAuth 2.0 service provider.
	 * 
	 * @param app
	 * @return
	 */
	public CompletionStage<ApiResponse<OAuthApp>> createOAuthApp(OAuthApp app) {
		return doApiPost(getOAuthAppsRoute(), app, OAuthApp.class);
	}

	/**
	 * gets a page of registered OAuth 2.0 client applications with Mattermost
	 * acting as an OAuth 2.0 service provider.
	 * 
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<List<OAuthApp>>> getOAuthApps(int page, int perPage) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getOAuthAppsRoute() + query, null, listType());
	}

	/**
	 * gets a registered OAuth 2.0 client application with Mattermost acting as
	 * an OAuth 2.0 service provider.
	 * 
	 * @param appId
	 * @return
	 */
	public CompletionStage<ApiResponse<OAuthApp>> getOAuthApp(String appId) {
		return doApiGet(getOAuthAppRoute(appId), null, OAuthApp.class);
	}

	/**
	 * gets a sanitized version of a registered OAuth 2.0 client application
	 * with Mattermost acting as an OAuth 2.0 service provider.
	 * 
	 * @param appId
	 * @return
	 */
	public CompletionStage<ApiResponse<OAuthApp>> getOAuthAppInfo(String appId) {
		return doApiGet(getOAuthAppRoute(appId) + "/info", null, OAuthApp.class);
	}

	/**
	 * deletes a registered OAuth 2.0 client application.
	 * 
	 * @param appId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteOAuthApp(String appId) {
		return doApiDelete(getOAuthAppRoute(appId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * regenerates the client secret for a registered OAuth 2.0 client
	 * application.
	 * 
	 * @param appId
	 * @return
	 */
	public CompletionStage<ApiResponse<OAuthApp>> regenerateOAuthAppSecret(String appId) {
		return doApiPost(getOAuthAppRoute(appId) + "/regen_secret", null, OAuthApp.class);
	}

	/**
	 * gets a page of OAuth 2.0 client applications the user authorized to use
	 * access their account.
	 * 
	 * @param userId
	 * @param page
	 * @param perPage
	 * @return
	 */
	public CompletionStage<ApiResponse<List<OAuthApp>>> getAuthorizedPAuthAppsForUser(String userId, int page,
			int perPage) {
		String query = String.format("?page=%d&per_page=%d", page, perPage);
		return doApiGet(getUserRoute(userId) + "/oauth/apps/authorized" + query, null, listType());
	}

	/**
	 * will authorize an OAuth 2.0 client application to access a user's account
	 * and provide a redirect link to follow.
	 * 
	 * @param authRequest
	 * @return
	 */
	public CompletionStage<String> authorizeOAuthApp(AuthorizeRequest authRequest) {
		return doApiRequest(HttpMethod.POST, url + "/oauth/authorize", authRequest, null, stringMapType())
				.thenApply(r -> r.readEntity())
				.thenApply(m -> m.get("redirect"));
	}

	/**
	 * will deauthorize an OAuth 2.0 client application from accessing a user's
	 * account.
	 * 
	 * @param appId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deauthorizeOAuthApp(String appId) {
		DeauthorizeOAuthAppRequest request = DeauthorizeOAuthAppRequest.builder().clientId(appId).build();
		return doApiRequest(HttpMethod.POST, url + "/oauth/deauthorize", request, null)
				.thenApply(this::checkStatusOK);
	}

	// Commands Section

	/**
	 * will create a new command if the user have the right permissions.
	 * 
	 * @param cmd
	 * @return
	 */
	public CompletionStage<ApiResponse<Command>> createCommand(Command cmd) {
		return doApiPost(getCommandsRoute(), cmd, Command.class);
	}

	/**
	 * updates a command based on the provided Command object.
	 * 
	 * @param cmd
	 * @return
	 */
	public CompletionStage<ApiResponse<Command>> updateCommand(Command cmd) {
		return doApiPut(getCommandRoute(cmd.getId()), cmd, Command.class);
	}

	/**
	 * deletes a command based on the provided command id string.
	 * 
	 * @param commandId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteCommand(String commandId) {
		return doApiDelete(getCommandRoute(commandId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * will retrieve a list of commands available in the team.
	 * 
	 * @param teamId
	 * @param customOnly
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Command>>> listCommands(String teamId, boolean customOnly) {
		String query = String.format("?team_id=%s&custom_only=%b", teamId, customOnly);
		return doApiGet(getCommandsRoute() + query, null, listType());
	}

	/**
	 * executes a given command.
	 * 
	 * @param channelId
	 * @param command
	 * @return
	 */
	public CompletionStage<ApiResponse<CommandResponse>> executeCommand(String channelId, String command) {
		CommandArgs args = new CommandArgs();
		args.setChannelId(channelId);
		args.setCommand(command);
		return doApiPost(getCommandsRoute() + "/execute", args, CommandResponse.class);
	}

	/**
	 * will retrieve a list of commands available in the team.
	 * 
	 * @param teamId
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Command>>> listAutocompleteCommands(String teamId) {
		return doApiGet(getTeamAutoCompleteCommandsRoute(teamId), null, listType());
	}

	/**
	 * will create a new token if the user have the right permissions.
	 * 
	 * @param commandId
	 * @return
	 */
	public CompletionStage<ApiResponse<String>> regenCommandToken(String commandId) {
		return doApiPut(getCommandRoute(commandId) + "/regen_token", null, String.class);
	}

	// Status Section

	/**
	 * returns a user status based on the provided user id string.
	 * 
	 * @param userId
	 * @param etag
	 * @return
	 */
	public CompletionStage<ApiResponse<Status>> getUserStatus(String userId, String etag) {
		return doApiGet(getUserStatusRoute(userId), etag, Status.class);
	}

	/**
	 * returns a list of users status based on the provided user ids.
	 * 
	 * @param userIds
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Status>>> getUsersStatusesByIds(String... userIds) {
		return doApiPost(getUserStatusesRoute() + "/ids", userIds, listType());
	}

	/**
	 * sets a user's status based on the provided user id string.
	 * 
	 * @param userId
	 * @param userStatus
	 * @return
	 */
	public CompletionStage<ApiResponse<Status>> updateUserStatus(String userId, Status userStatus) {
		return doApiPut(getUserStatusRoute(userId), userStatus, Status.class);
	}

	// Webrtc Section

	/**
	 * returns a valid token, stun server and turn server with credentials to
	 * use with the Mattermost WebRTC service.
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<WebrtcInfoResponse>> getWebrtcToken() {
		return doApiGet("/webrtc/token", null, WebrtcInfoResponse.class);
	}

	// Emoji Section

	/**
	 * will save an emoji to the server if the current user has permission to do
	 * so. If successful, the provided emoji will be returned with its Id field
	 * filled in. Otherwise, an error will be returned.
	 * 
	 * @param emoji
	 * @param imageFile
	 * @param fileName
	 * @return
	 */
	public CompletionStage<ApiResponse<Emoji>> createEmoji(Emoji emoji, Path imageFile, String fileName) {
		throw new UnsupportedOperationException("not impl"); // FIXME
	}

	/**
	 * returns a list of custom emoji in the syste,/
	 * 
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Emoji>>> getEmojiList() {
		return doApiGet(getEmojisRoute(), null, listType());
	}

	/**
	 * delete an custom emoji on the provided emoji id string.
	 * 
	 * @param emojiId
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteEmoji(String emojiId) {
		return doApiDelete(getEmojiRoute(emojiId))
				.thenApply(this::checkStatusOK);
	}

	/**
	 * returns a custom emoji in the system on the provided emoji id string.
	 * 
	 * @param emojiId
	 * @return
	 */
	public CompletionStage<ApiResponse<Emoji>> getEmoji(String emojiId) {
		return doApiGet(getEmojiRoute(emojiId), null, Emoji.class);
	}

	/**
	 * returns the emoji image.
	 * 
	 * @param emojiId
	 * @return
	 */
	public CompletionStage<ApiResponse<Object>> getEmojiImage(String emojiId) {
		throw new UnsupportedOperationException("not impl");
	}

	// Reaction Section

	/**
	 * saves an emoji reaction for a post. Returns the saved reaction if
	 * successful, otherwise an error will be returned.
	 * 
	 * @param reaction
	 * @return
	 */
	public CompletionStage<ApiResponse<Reaction>> saveReaction(Reaction reaction) {
		return doApiPost(getReactionsRoute(), reaction, Reaction.class);
	}

	/**
	 * returns a list of reactions to a post.
	 * 
	 * @param postId
	 * @return
	 */
	public CompletionStage<ApiResponse<List<Reaction>>> getReactions(String postId) {
		return doApiGet(getPostRoute(postId) + "/reactions", null, listType());
	}

	/**
	 * deletes reaction of a user in a post.
	 * 
	 * @param reaction
	 * @return
	 */
	public CompletionStage<ApiResponse<Boolean>> deleteReaction(Reaction reaction) {
		return doApiDelete(getUserRoute(reaction.getUserId()) + getPostRoute(reaction.getPostId())
				+ String.format("/reactions/%s", reaction.getEmojiName()))
						.thenApply(this::checkStatusOK);
	}
}