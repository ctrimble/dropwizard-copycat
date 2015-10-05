package com.xiantrimble.dropwizard.copycat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import io.atomix.catalyst.transport.Address;
import io.atomix.copycat.client.Command;
import io.atomix.copycat.client.Query;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.StateMachineExecutor;

public class CreateStateMachineTest {
  @Rule public CopycatClusterRule<MyStateMachine> clusterRule = new CopycatClusterRule<MyStateMachine>()
		  .withServerCount(5)
		  .withClientCount(1)
		  .withInitalPort(9000)
		  .withStoragePath("target/test/cluster")
		  .withStateMachine(MyStateMachine::new);
  
  @Test
  public void callCommandAndQuery() throws InterruptedException, ExecutionException, RuntimeException {
	  clusterRule.getClient(0).submit(new PutCommand().withValue("data")).get();
	  Object value = clusterRule.getClient(0).submit(new GetQuery()).get();
	  
	  assertThat(value, equalTo("data"));
  }
  
  @Test
  public void commandAndQueryAddServer() throws InterruptedException, ExecutionException, RuntimeException {
	  clusterRule.getClient(0).submit(new PutCommand().withValue("data")).get();
	  clusterRule.addServer();
	  Object value = clusterRule.getClient(0).submit(new GetQuery()).get();	  
	  assertThat(value, equalTo("data"));
  }
  
  @Test
  public void commandAndQueryLeaderChange() throws InterruptedException, ExecutionException, RuntimeException {
	  clusterRule.getClient(0).submit(new PutCommand().withValue("data")).get();
	  clusterRule.stopLeader(10, TimeUnit.SECONDS);
	  Object value = clusterRule.getClient(0).submit(new GetQuery()).get();	  
	  assertThat(value, equalTo("data"));
  }
  
  @Test
  public void leaderAddressChange() throws InterruptedException, ExecutionException, RuntimeException {
	  Optional<Address> initialLeader = clusterRule.getLeader();
	  assertThat(initialLeader.isPresent(), equalTo(true));
	  clusterRule.stopLeader(100, TimeUnit.SECONDS);
	  Optional<Address> nextLeader = clusterRule.getLeader();
	  assertThat(nextLeader.isPresent(), equalTo(true));
	  assertThat(nextLeader.get(), not(equalTo(initialLeader.get())));
  }
  
  public static class MyStateMachine extends StateMachine {
	  
	Commit<PutCommand> value;
	  
	private void put(Commit<PutCommand> command) {
		if( value != null ) {
			value.clean();
		}
		value = (Commit<PutCommand>)command;
	}
	
	private Object get(Commit<GetQuery> command) {
		try {
		  return value != null ? value.operation().getValue() : null;
		}
		finally {
			command.close();
		}
	}

	@Override
	protected void configure(StateMachineExecutor executor) {
		executor.register(PutCommand.class, this::put);
		executor.register	(GetQuery.class, this::get);
	}
  }
  
  public static class PutCommand implements Command<Void> {
	private static final long serialVersionUID = 1L;
	private Object value;
	public PutCommand withValue(Object value) {
		this.value = value;
		return this;
	}
	public Object getValue() {
		return value;
	}
  }
  
  public static class GetQuery implements Query<Object> {
		private static final long serialVersionUID = 1L;	  
  }
}
