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
import java.util.function.Consumer;

import com.google.common.collect.Maps;

import io.atomix.copycat.client.Command;
import io.atomix.copycat.client.Operation;
import io.atomix.copycat.client.Query;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.StateMachineExecutor;

/**
 * An example state machine.
 * @author ctrimble
 *
 */
public class ExampleStateMachine extends StateMachine {

	Map<String, ClusterNode> cluster = Maps.newHashMap();
	
	Runnable update;
	
	@Override
	public void configure(StateMachineExecutor executor) {
		executor.register(SetCommand.class, this::set);
		executor.register(GetQuery.class, this::get);
	}
	
	public void set( Commit<SetCommand> command ) {
		cluster.put(command.operation().getNode().getId(), command.operation().getNode());
	}
	
	public ClusterNode get( Commit<GetQuery> query ) {
		return cluster.get(query.operation().getId());
	}
	
	public static class SetCommand implements Command<Void> {
		public ClusterNode getNode() {
			return node;
		}
		public void setNode(ClusterNode node) {
			this.node = node;
		}
		private static final long serialVersionUID = 1L;
		private ClusterNode node;

    }
	
	public static class GetQuery implements Query<ClusterNode> {
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		private static final long serialVersionUID = 1L;
		private String id;
		
	}

}
