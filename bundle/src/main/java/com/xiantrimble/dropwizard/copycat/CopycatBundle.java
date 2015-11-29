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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiantrimble.jackson.catalyst.CatalystInputStream;
import com.xiantrimble.jackson.catalyst.CatalystOutputStream;

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
import io.atomix.catalyst.buffer.BufferDataOutput;
import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.SerializableTypeResolver;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.SerializerRegistry;
import io.atomix.catalyst.serializer.TypeSerializer;
import io.atomix.catalyst.serializer.TypeSerializerFactory;

public class CopycatBundle<C extends Configuration> implements ConfiguredBundle<C> {

  public static class Builder<C extends Configuration> {

    private Function<C, CopycatConfiguration> configuration;
	private Supplier<StateMachine> stateMachineSupplier;
	private Serializer serializer;

    public CopycatBundle<C> build() {
    	if( configuration == null ) {
    		throw new IllegalArgumentException("configuraiton accessor is required.");
    	}
      return new CopycatBundle<C>(configuration, stateMachineSupplier, serializer);
    }

    public Builder<C> withConfiguration(Function<C, CopycatConfiguration> configuration) {
      this.configuration = configuration;
      return this;
    }
    
    public Builder<C> withStateMachineSupplier( Supplier<StateMachine> stateMachineSupplier ) {
    	this.stateMachineSupplier = stateMachineSupplier;
    	return this;
    }
    
    public Builder<C> withSerializer( Serializer serializer ) {
    	this.serializer = serializer;
    	return this;
    }
  }
  
  public static <T> TypeSerializer<T> objectMapperSerializer(ObjectMapper mapper) {
	  return new TypeSerializer<T>() {

		@Override
		public void write(T object, BufferOutput buffer, Serializer serializer) {
			try (CatalystOutputStream out = new CatalystOutputStream(buffer)) {
				mapper.writeValue(out, object);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public T read(Class<T> type, BufferInput buffer, Serializer serializer) {
			try (CatalystInputStream in = new CatalystInputStream(buffer)){
				return mapper.readValue(in, type);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

	};
  }

  private Function<C, CopycatConfiguration> configurationAccessor;
  private CopycatConfiguration configuration;
  CopycatServer server;
  CopycatClient client;
  private Supplier<StateMachine> stateMachineSupplier;
  private ObjectMapper mapper;
  private Serializer serializer;

  public CopycatBundle(Function<C, CopycatConfiguration> configurationAccessor, Supplier<StateMachine> stateMachineSupplier, Serializer serializer) {
	this.configurationAccessor = configurationAccessor;
	this.stateMachineSupplier = stateMachineSupplier;
	this.serializer = serializer;
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
		server.start().get();
	}

	@Override
	public void stop() throws Exception {
		System.out.println("stopping copycat");
		if( server != null ) {
			server.stop().get();
		}
	} 
  }
  
  public class JacksonSerializer extends Serializer {
	  
  }
  
  public class CopycatClientManager implements Managed {

		@Override
		public void start() throws Exception {
			System.out.println("starting copycat client");
			client.connect().join();
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
	Transport transport = new NettyTransport();
	File logs = new File(configuration.getLog());
	logs.mkdirs();
	Storage storage = Storage.builder().withDirectory(logs).build();
	  
    CopycatServer.Builder builder = CopycatServer.builder(new Address(configuration.getAddress().getHost(), configuration.getAddress().getPort()), configuration.members())
            .withTransport(transport)
            .withStorage(storage)
            .withStateMachine(stateMachineSupplier);
    
    if( serializer != null ) {
            builder.withSerializer(serializer);
    }

    return builder.build();
  }

  public CopycatClient createClient() {
	CopycatClient.Builder builder = CopycatClient.builder(configuration.address())
			.withTransport(new NettyTransport());
	
	if( serializer != null ) {
		builder.withSerializer(serializer);
	}
	
	client = builder.build();
	
	return client;
  }

}
