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

import com.xiantrimble.dropwizard.copycat.CopycatBundle;
import com.xiantrimble.dropwizard.copycat.guice.CopycatModule;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Stage;
import com.hubspot.dropwizard.guice.GuiceBundle;

import io.atomix.copycat.server.CopycatServer;
import io.dropwizard.Application;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * copycat-example entry point
 */
public class CopycatExampleApplication extends Application<CopycatExampleConfiguration> {

  public static final String PASSPHRASE_ENV_VAR = "PASSPHRASE";
  public static final String COMMAND_NAME = "copycat-example";

  public static void main(String[] args) throws Exception {
    new CopycatExampleApplication().run(args);
  }

  protected GuiceBundle<CopycatExampleConfiguration> guiceBundle;
  protected CopycatBundle<CopycatExampleConfiguration> bundle;

  public CopycatExampleApplication() {
  }

  @Override
  public String getName() {
    return COMMAND_NAME;
  }

  @Override
  public void initialize(Bootstrap<CopycatExampleConfiguration> bootstrap) {
    bootstrap.addBundle(bundle =
        CopycatBundle.<CopycatExampleConfiguration> builder()
            .withConfiguration(CopycatExampleConfiguration::getCopycat)
            .withStateMachineSupplier(ExampleStateMachine::new).build());

    GuiceBundle.Builder<CopycatExampleConfiguration> builder =
        GuiceBundle.<CopycatExampleConfiguration> newBuilder()
            .setConfigClass(CopycatExampleConfiguration.class)
            .enableAutoConfig(getClass().getPackage().getName());
    builder.addModule(new CopycatExampleModule());
    builder.addModule(new CopycatModule(bundle));

    bootstrap.addBundle(guiceBundle = builder.build(Stage.DEVELOPMENT));
    
    super.initialize(bootstrap);
  }

  @Override
  public void run(CopycatExampleConfiguration configuration, Environment environment)
      throws Exception {
  }
  
  @Singleton
  public static class CompucationManaged implements Managed {
	  
	  @Inject
	  Supplier<CopycatServer> copycat;

		@Override
		public void start() throws Exception {
			System.out.println("starting computation");
		}

		@Override
		public void stop() throws Exception {
			System.out.println("stopping computation");
		}
  }
}
