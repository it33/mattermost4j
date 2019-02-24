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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;

/**
 * TODO 型の説明
 * 
 * @author Takayuki Maruyama
 */
class HandlerRegistry {

  private final EnumMap<WebSocketEvent, Collection<WebSocketEventHandler>> eventHandlers =
      new EnumMap<>(WebSocketEvent.class);

  public void fireEvent(WebSocketEvent eventType, String data, Broadcast broadcast) {
    Collection<WebSocketEventHandler> handlers = ensureHandlersForType(eventType);
    handlers.forEach(h -> h.handleEvent(data, broadcast));
  }

  public HandlerRegistration addHandler(WebSocketEvent eventType, WebSocketEventHandler handler) {
    Collection<WebSocketEventHandler> handlers = ensureHandlersForType(eventType);
    handlers.add(handler);
    return new HandlerRegistration(() -> handlers.remove(handler));
  }

  protected Collection<WebSocketEventHandler> ensureHandlersForType(WebSocketEvent eventType) {
    return eventHandlers.computeIfAbsent(eventType, e -> new ArrayList<>());
  }

  @FunctionalInterface
  interface WebSocketEventHandler {
    void handleEvent(String data, Broadcast broadcast);
  }

}
