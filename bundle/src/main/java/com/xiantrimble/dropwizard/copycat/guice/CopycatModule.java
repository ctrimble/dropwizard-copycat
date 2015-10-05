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
package com.xiantrimble.dropwizard.copycat.guice;

import com.xiantrimble.dropwizard.copycat.CopycatBundle;

import java.util.function.Supplier;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import io.dropwizard.Configuration;
import io.atomix.copycat.server.CopycatServer;

public class CopycatModule extends AbstractModule {

  private CopycatBundle<? extends Configuration> bundle;

  public CopycatModule(CopycatBundle<? extends Configuration> bundle) {
    this.bundle = bundle;
  }

  @Override
  protected void configure() {
  }
  
  @Provides
  public Supplier<CopycatServer> server() {
	  return bundle::getServer;
  }

}
