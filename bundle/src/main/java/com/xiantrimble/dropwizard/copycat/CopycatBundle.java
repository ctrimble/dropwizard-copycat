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
package com.xiantrimble.dropwizard.copycat;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.catalyst.transport.Transport;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.catalyst.serializer.Serializer;

public class CopycatBundle<C extends Configuration> implements ConfiguredBundle<C> {

  public static class Builder<C extends Configuration> {

    private Function<C, CopycatConfiguration> configuration;
	private Supplier<StateMachine> stateMachineSupplier;

    public CopycatBundle<C> build() {
    	if( configuration == null ) {
    		throw new IllegalArgumentException("configuraiton accessor is required.");
    	}
      return new CopycatBundle<C>(configuration, stateMachineSupplier);
    }

    public Builder<C> withConfiguration(Function<C, CopycatConfiguration> configuration) {
      this.configuration = configuration;
      return this;
    }
    
    public Builder<C> withStateMachineSupplier( Supplier<StateMachine> stateMachineSupplier ) {
    	this.stateMachineSupplier = stateMachineSupplier;
    	return this;
    }
  }

  private Function<C, CopycatConfiguration> configurationAccessor;
  private CopycatConfiguration configuration;
  CopycatServer server;
  CopycatClient client;
  private Supplier<StateMachine> stateMachineSupplier;
  private ObjectMapper mapper;
  private Serializer serializer;

  public CopycatBundle(Function<C, CopycatConfiguration> configurationAccessor, Supplier<StateMachine> stateMachineSupplier) {
	this.configurationAccessor = configurationAccessor;
	this.stateMachineSupplier = stateMachineSupplier;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    
  }

  @Override
  public void run(C appConfig, Environment environment) throws Exception {
    configuration = configurationAccessor.apply(appConfig);
    
    if( configuration == null ) {
    	throw new NullPointerException("configuration cannot be null.");
    }
    if( stateMachineSupplier != null ) {
      server = createServer(stateMachineSupplier);
      environment.lifecycle().manage(new CopycatServerManager());
    }
    client = createClient();
    environment.lifecycle().manage(new CopycatClientManager());
  }

  public CopycatServer getServer() {
    return server;
  }
  
  public CopycatClient getClient() {
	  return client;
  }

  public static <C extends Configuration> Builder<C> builder() {
    return new Builder<C>();
  }
  
  public class CopycatServerManager implements Managed {

	@Override
	public void start() throws Exception {
		System.out.println("starting copycat");
		server.open().get();
	}

	@Override
	public void stop() throws Exception {
		System.out.println("stopping copycat");
		if( server != null ) {
			server.close().get();
		}
	} 
  }
  
  public class JacksonSerializer extends Serializer {
	  
  }
  
  public class CopycatClientManager implements Managed {

		@Override
		public void start() throws Exception {
			System.out.println("starting copycat client");
			client.open().join();
		}

		@Override
		public void stop() throws Exception {
			System.out.println("stopping copycat client");
			if( client != null ) {
				client.close().join();
			}
		} 	  
  }

  public CopycatServer createServer(Supplier<StateMachine> stateMachineSupplier) {
	Transport transport = new NettyTransport(5);
	File logs = new File(configuration.getLog());
	logs.mkdirs();
	Storage storage = Storage.builder().withDirectory(logs).build();
	  
    return CopycatServer.builder(new Address(configuration.getAddress().getHost(), configuration.getAddress().getPort()), configuration.members())
            .withTransport(transport)
            .withStorage(storage)
            .withStateMachine(stateMachineSupplier.get())
            .withSerializer(serializer)
    		.build();
  }

  public CopycatClient createClient() {
	CopycatClient client = CopycatClient.builder(configuration.address())
			.withTransport(new NettyTransport(5))
			.build();
	
	return client;
  }

}
