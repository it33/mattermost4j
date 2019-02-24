/*
 * Copyright (c) 2019 Takayuki Maruyama
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.bis5.mattermost.websocket;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import lombok.RequiredArgsConstructor;
import net.bis5.mattermost.jersey.provider.MattermostModelMapperProvider;

/**
 * TODO 型の説明
 * 
 * @author Takayuki Maruyama
 */
@ClientEndpoint
@RequiredArgsConstructor
public class MattermostWebSocketClient {

  private final String authToken;
  private final HandlerRegistry handlers = new HandlerRegistry();
  private final ObjectMapper objectMapper = new MattermostModelMapperProvider().getContext(null);

  @OnOpen
  protected void onOpen(Session session) throws IOException {
    authenticate(session);
  }

  private void authenticate(Session session) throws IOException {
    // TODO ちゃんとモデル作る
    String json =
        "{\"seq\": 1, \"action\": \"authentication_challenge\", \"data\": {\"token\": \"%s\"}}";
    json = String.format(json, authToken);
    session.getBasicRemote().sendText(json);
  }

  @OnClose
  public void onClose(Session session) {
    // TODO
  }

  @OnError
  public void onError(Throwable thrown) {
    // TODO
  }

  @OnMessage
  public void onMessage(String message)
      throws JsonParseException, JsonMappingException, IOException {
    EventPayload payload = objectMapper.readValue(message, EventPayload.class);
    handlers.fireEvent(payload.getEvent(), payload.getData(), payload.getBroadcast());
  }
}
