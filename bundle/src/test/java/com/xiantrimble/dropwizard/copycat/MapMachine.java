package com.xiantrimble.dropwizard.copycat;

import java.util.HashMap;
import java.util.Map;

import io.atomix.copycat.Command;
import io.atomix.copycat.Query;
import io.atomix.copycat.Query.ConsistencyLevel;
import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.Snapshottable;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.session.ServerSession;
import io.atomix.copycat.server.session.SessionListener;
import io.atomix.copycat.server.storage.snapshot.SnapshotReader;
import io.atomix.copycat.server.storage.snapshot.SnapshotWriter;

public class MapMachine {

  public static class TestStateMachine extends StateMachine implements SessionListener, Snapshottable {
    private Map<String, String> values = new HashMap<String, String>();
  
    @Override
    public void register(ServerSession session) {
  
    }
  
    @Override
    public void unregister(ServerSession session) {
  
    }
  
    @Override
    public void snapshot(SnapshotWriter writer) {
      writer.writeObject(values);
    }
  
    @Override
    public void install(SnapshotReader reader) {
      values = reader.readObject();
    }
  
    public String put(Commit<MapMachine.TestPut> put) {
      try {
        return values.put(put.command().key(), put.command().value());
      } finally {
        put.release();
      }
    }
  
    public String get(Commit<MapMachine.TestGet> commit) {
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
    private String value;
    private String key;
  
    public TestPut(String key, String value) {
      this.key = key;
      this.value = value;
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
    private String key;
    private ConsistencyLevel consistency;
  
    public TestGet(String key, ConsistencyLevel consistency) {
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
