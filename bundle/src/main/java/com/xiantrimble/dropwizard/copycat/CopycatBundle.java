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

import java.util.function.Function;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.kuujo.copycat.Copycat;
import net.kuujo.copycat.CopycatServer;

;

public class CopycatBundle<C extends Configuration> implements ConfiguredBundle<C> {

  public static class Builder<C extends Configuration> {

    private Function<C, CopycatConfiguration> configuration;

    public CopycatBundle<C> build() {
    	if( configuration == null ) {
    		throw new IllegalArgumentException("configuraiton accessor is required.");
    	}
      return new CopycatBundle<C>(configuration);
    }

    public Builder<C> withConfiguration(Function<C, CopycatConfiguration> configuration) {
      this.configuration = configuration;
      return this;
    }
  }

  private Function<C, CopycatConfiguration> configurationAccessor;
  private CopycatConfiguration configuration;
  CopycatServer server;
  Copycat copycat;

  public CopycatBundle(Function<C, CopycatConfiguration> configurationAccessor) {
	this.configurationAccessor = configurationAccessor;
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
    server = configuration.createServer();
    System.out.println("Managing server");
    environment.lifecycle().manage(new CopycatServerManager());
  }

  public CopycatServer getServer() {
    return server;
  }
  
  public Copycat getCopycat() {
	  return copycat;
  }

  public static <C extends Configuration> Builder<C> builder() {
    return new Builder<C>();
  }
  
  public class CopycatServerManager implements Managed {

	@Override
	public void start() throws Exception {
		System.out.println("starting copycat");
		copycat = server.open().get();
	}

	@Override
	public void stop() throws Exception {
		if( server != null ) {
			server.close().get();
		}
	}
	  
  }

}
