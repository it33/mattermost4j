/*
 * @(#) net.bis5.mattermost.client4.api.PreferencesApi
 * Copyright (c) 2017 Maruyama Takayuki
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
package net.bis5.mattermost.client4.api;

import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.model.Preference;
import net.bis5.mattermost.model.Preferences;

/**
 * TODO 型の説明
 * 
 * @author Maruyama Takayuki
 * @since 2017/09/09
 */
public interface PreferencesApi {

	ApiResponse<Preferences> getPreferences(String userId);

	boolean updatePreferences(String userId, Preferences perferences);

	boolean deletePreferences(String userId, Preferences preferences);

	ApiResponse<Preferences> getPreferencesByCategory(String userId, String category);

	ApiResponse<Preference> getPreferenceByCategoryAndName(String userId, String category, String preferenceName);
}
