package com.xiantrimble.dropwizard.copycat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.atomix.catalyst.buffer.BufferInputStream;
import io.atomix.catalyst.buffer.BufferOutputStream;
import io.atomix.copycat.Command;
import io.atomix.copycat.Query;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class ObjectMapperMachine {

  public static class TestStateMachine extends StateMachine implements SessionListener, Snapshottable {
    public static ObjectMapper MAPPER = new ObjectMapper()
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    
    @JsonProperty
    private Map<String, String> values = new HashMap<String, String>();
  
    @Override
    public void register(ServerSession session) {
  
    }
  
    @Override
    public void unregister(ServerSession session) {
  
    }
  
    @Override
    public void snapshot(SnapshotWriter writer) {
      try {
        MAPPER.writeValue(new BufferOutputStream(writer), this);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  
    @Override
    public void install(SnapshotReader reader) {
      try {
        MAPPER.readerForUpdating(this).readValue(new BufferInputStream(reader));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  
    public String put(Commit<ObjectMapperMachine.TestPut> put) {
      try {
        return values.put(put.command().key(), put.command().value());
      } finally {
        put.release();
      }
    }
  
    public String get(Commit<ObjectMapperMachine.TestGet> commit) {
      try {
        return values.get(commit.command().key());
      } finally {
        commit.release();
      }
    }
  
    @Override
    public void expire(ServerSession session) {
      // TODO Auto-generated method stub
      
    }
  
    @Override
    public void close(ServerSession session) {
      // TODO Auto-generated method stub
      
    }
  }

  /**
   * Test command.
   */
  public static class TestPut implements Command<String> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @JsonProperty("value")
    private String value;
    @JsonProperty("key")
    private String key;
    @JsonProperty("consistency")
    private ConsistencyLevel consistency;
  
    @JsonCreator
    public TestPut(@JsonProperty("key") String key, @JsonProperty("value") String value, @JsonProperty("consistency") ConsistencyLevel consistency) {
      this.key = key;
      this.value = value;
      this.consistency = consistency;
    }
  
    @Override
    public ConsistencyLevel consistency() {
      return consistency;
    }
  
    @Override
    public CompactionMode compaction() {
      return CompactionMode.QUORUM;
    }
  
    public String value() {
      return value;
    }
    
    public String key() {
      return key;
    }
  }

  /**
   * Test query.
   */
  public static class TestGet implements Query<String> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    @JsonProperty("key")
    private String key;
    @JsonProperty("consistency")
    private ConsistencyLevel consistency;
  
    @JsonCreator
    public TestGet(@JsonProperty("key") String key, @JsonProperty("consistency") ConsistencyLevel consistency) {
      this.key = key;
      this.consistency = consistency;
    }
  
    @Override
    public ConsistencyLevel consistency() {
      return consistency;
    }
  
    public String key() {
      return key;
    }
  }

}
