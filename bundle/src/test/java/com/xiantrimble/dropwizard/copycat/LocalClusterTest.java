package com.xiantrimble.dropwizard.copycat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

import io.atomix.copycat.Command;
import io.atomix.copycat.Query;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer.State;

/**
 * A set of tests for a very basic Copycat setup.
 * 
 * @author Christian Trimble
 *
 */
public class LocalClusterTest {
  public @Rule CopycatClusterRule<MapMachine.TestStateMachine> clusterRule = CopycatClusterRule.<MapMachine.TestStateMachine>builder()
      .withStateMachineSupplier(MapMachine.TestStateMachine::new)
      .build();
  
  @Test
  public void oneLeaderTwoFollowers() throws Throwable {
    
    Map<State, Integer> states = clusterRule.getServers().stream().collect(Collectors.toMap(s->s.state(), s->1, (s1, s2)->s1+s2));
    
    assertThat(states.get(State.LEADER), equalTo(1));
    assertThat(states.get(State.FOLLOWER), equalTo(2));
  }
  
  @Test
  public void stateMachineCommand() throws Throwable {
    
    CopycatClient client = clusterRule.createClient();
    AtomicReference<String> valueRef = new AtomicReference<String>();
    
    CountDownLatch clientLatch = new CountDownLatch(1);
    client.submit(new MapMachine.TestPut("key", "value"))
    .thenAccept(value->{
      valueRef.set(value);
      clientLatch.countDown();
    });
    
    assertThat(clientLatch.await(10, TimeUnit.SECONDS), equalTo(true));
    assertThat(valueRef.get(), nullValue());
  }
  
  @Test
  public void stateMachineCommands() throws Throwable {
    
    CopycatClient client = clusterRule.createClient();
    AtomicReference<String> valueRef = new AtomicReference<String>();
    
    CountDownLatch clientLatch = new CountDownLatch(1);
    client.submit(new MapMachine.TestPut("key", "value1"))
    .thenRun(()->{
      client.submit(new MapMachine.TestGet("key", Query.ConsistencyLevel.SEQUENTIAL))
      .thenAccept(value->{
        valueRef.set(value);
        clientLatch.countDown();
      });
    });
    
    assertThat(clientLatch.await(10, TimeUnit.SECONDS), equalTo(true));
    assertThat(valueRef.get(), equalTo("value1"));
  }
}
