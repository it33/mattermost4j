/*
 * @(#) net.bis5.mattermost.client4.api.EmojiApi
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

import java.nio.file.Path;
import java.util.List;

import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.model.Emoji;

/**
 * TODO 型の説明
 * 
 * @author Maruyama Takayuki
 * @since 2017/09/09
 */
public interface EmojiApi {

	ApiResponse<Emoji> createEmoji(Emoji emoji, Path imageFile, String fileName);

	ApiResponse<List<Emoji>> getEmojiList();

	ApiResponse<Boolean> deleteEmoji(String emojiId);

	ApiResponse<Emoji> getEmoji(String emojiId);

	ApiResponse<Object> getEmojiImage(String emojiId);
}
