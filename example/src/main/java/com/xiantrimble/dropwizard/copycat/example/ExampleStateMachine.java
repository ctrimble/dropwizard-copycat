/**
 * Copyright (C) 2015 Christian Trimble (xiantrimble@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiantrimble.dropwizard.copycat.example;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.copycat.client.Command;
import io.atomix.copycat.client.Operation;
import io.atomix.copycat.client.Query;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.StateMachineExecutor;

/**
 * An example state machine.
 * @author Christian Trimble
 *
 */
public class ExampleStateMachine extends StateMachine {
	private static ObjectMapper mapper = new ObjectMapper();

	Map<String, JsonNode> values = Maps.newHashMap();
	
	Runnable update;
	
	@Override
	public void configure(StateMachineExecutor executor) {
		executor.register(SetValue.class, this::set);
		executor.register(GetValue.class, this::get);
		executor.register(GetKeys.class, this::keySet);
	}
	
	public void set( Commit<SetValue> command ) {
		values.put(command.operation().getKey(), command.operation().getNode());
	}
	
	public JsonNode get( Commit<GetValue> query ) {
		return values.get(query.operation().getKey());
	}
	
	public Set<String> keySet( Commit<GetKeys> query ) {
		return values.keySet();
	}
	
	public static class SetValue implements Command<Void>, CatalystSerializable {
		private static final long serialVersionUID = 1L;
		private JsonNode node;
		private String key;
		public JsonNode getNode() {
			return node;
		}
		public void setNode(JsonNode node) {
			this.node = node;
		}
		public SetValue withNode(JsonNode node) {
			this.node = node;
			return this;
		}
		public String getKey() {
			return key;
		}
		public void setKey( String key ) {
			this.key = key;
		}
		public SetValue withKey( String key ) {
			this.key = key;
			return this;
		}

		@Override
		public void readObject(BufferInput<?> input, Serializer serializer) {
			key = serializer.readObject(input);
			node = serializer.readObject(input);
		}

		@Override
		public void writeObject(BufferOutput<?> output, Serializer serializer) {
			serializer.writeObject(key, output);
			serializer.writeObject(node, output);
		}
    }
	
	public static class GetValue implements Query<JsonNode> {
		private static final long serialVersionUID = 1L;
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public GetValue withKey( String key ) {
			this.key = key;
			return this;
		}
		private String key;
	}
	
	public static class GetKeys implements Query<Set<String>> {
		private static final long serialVersionUID = 1L;
	}

}
