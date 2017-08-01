/*
 * @(#) net.bis5.mattermost.model.CommandMethod
 * Copyright (c) 2016 takayuki
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
package net.bis5.mattermost.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.bis5.mattermost.model.CommandMethod.CommandMethodDeserializer;
import net.bis5.mattermost.model.serialize.HasCodeSerializer;

/**
 * TODO 型の説明
 * 
 * @author takayuki
 * @since 2016/10/08
 */
@JsonSerialize(using = HasCodeSerializer.class)
@JsonDeserialize(using = CommandMethodDeserializer.class)
public enum CommandMethod implements HasCode<CommandMethod> {

	POST("P"), GET("G");
	private final String code;

	CommandMethod(String code) {
		this.code = code;
	}

	@Override
	public String getCode() {
		return code;
	}

	public static CommandMethod of(String code) {
		for (CommandMethod method : CommandMethod.values()) {
			if (method.getCode().equals(code)) {
				return method;
			}
		}
		return null;
	}

	public static class CommandMethodDeserializer extends JsonDeserializer<CommandMethod> {

		/**
		 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser,
		 *      com.fasterxml.jackson.databind.DeserializationContext)
		 */
		@Override
		public CommandMethod deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			String code = p.getText();
			return CommandMethod.of(code);
		}
	}
}