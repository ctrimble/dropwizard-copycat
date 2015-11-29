package com.xiantrimble.dropwizard.copycat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiantrimble.dropwizard.copycat.ObjectMapperMachine.TestStateMachine;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferInputStream;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.buffer.BufferOutputStream;
import io.atomix.catalyst.serializer.SerializationException;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.serializer.TypeSerializer;
import io.atomix.catalyst.serializer.TypeSerializerFactory;
import io.atomix.catalyst.transport.NettyTransport;
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
public class ObjectMapperClusterTest {

  public @Rule CopycatClusterRule<TestStateMachine> clusterRule = CopycatClusterRule.<TestStateMachine>builder()
      .withStateMachineSupplier(TestStateMachine::new)
      .withDirectoryStorage("target/copycat/storage")
      .withTransportSupplier(NettyTransport::new)
      .withSerializer(ObjectMapperClusterTest::objectMapperSerializer)
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
    client.submit(new ObjectMapperMachine.TestPut("key", "value", Command.ConsistencyLevel.SEQUENTIAL))
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
    client.submit(new ObjectMapperMachine.TestPut("key", "value1", Command.ConsistencyLevel.SEQUENTIAL))
    .thenRun(()->{
      client.submit(new ObjectMapperMachine.TestGet("key", Query.ConsistencyLevel.SEQUENTIAL))
      .thenAccept(value->{
        valueRef.set(value);
        clientLatch.countDown();
      });
    });
    
    assertThat(clientLatch.await(10, TimeUnit.SECONDS), equalTo(true));
    assertThat(valueRef.get(), equalTo("value1"));
  }
  
  public static Serializer objectMapperSerializer() {
    ObjectMapperTypeSerializerFactory factory = new ObjectMapperTypeSerializerFactory();
    return new Serializer()
        .register(ObjectMapperMachine.TestPut.class, factory)
        .register(ObjectMapperMachine.TestGet.class, factory);
  }
  
  public static class ObjectMapperTypeSerializerFactory implements TypeSerializerFactory {
    GenericJacksonSerializer serializer = new GenericJacksonSerializer();

    @Override
    public TypeSerializer<?> createSerializer(Class<?> type) {
      return serializer;
    }
  }
  
  public static class GenericJacksonSerializer implements TypeSerializer<Object> {
    private final ObjectMapper mapper;

    public GenericJacksonSerializer() {
      mapper = new ObjectMapper()
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void write(Object object, BufferOutput buffer, Serializer serializer) {
      try {
        mapper.writeValue(new BufferOutputStream(buffer), object);
      } catch (IOException e) {
        throw new SerializationException(e);
      }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object read(Class type, BufferInput buffer, Serializer serializer) {
      try {
        return mapper.readValue(new BufferInputStream(buffer), type);
      } catch (IOException e) {
        throw new SerializationException(e);
      }
    }

  }
}
