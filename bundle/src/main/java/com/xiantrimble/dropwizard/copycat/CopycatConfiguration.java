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
import java.util.List;
import java.util.function.Supplier;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.catalyst.transport.Transport;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.Storage;

public class CopycatConfiguration {
protected HostAndPort address;
  protected List<HostAndPort> members;
  protected String log;
  
  public void setAddress( HostAndPort address ) {
	  this.address = address;
  }
  
  public HostAndPort getAddress() {
	  return address;
  }

  public List<HostAndPort> getMembers() {
    return members;
  }

  public void setMembers(List<HostAndPort> members) {
    this.members = members;
  }
  
  public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}
	
	public static class HostAndPort {
		private String host;
		  private int port;
		  
		  public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
	}

  public CopycatServer createServer(Supplier<StateMachine> stateMachineSupplier) {
	  
	  Transport transport = new NettyTransport(5);
	  File logs = new File(log);
	  logs.mkdirs();
	  Storage storage = Storage.builder().withDirectory(logs).build();
	  
    return CopycatServer.builder(new Address(address.getHost(), address.getPort()), members())
            .withTransport(transport)
            .withStorage(storage)
            .withStateMachine(stateMachineSupplier.get())
    		.build();
  }

  public Address[] members() {
	return members.stream()
			.map(address->new Address(address.getHost(), address.getPort()))
			.toArray(size->new Address[size]);


  }
  
}
