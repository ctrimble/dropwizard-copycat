package com.xiantrimble.dropwizard.copycat;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.LocalServerRegistry;
import io.atomix.catalyst.transport.LocalTransport;
import io.atomix.catalyst.transport.Transport;
import io.atomix.catalyst.util.Listener;
import io.atomix.copycat.client.ConnectionStrategies;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.client.RetryStrategies;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.cluster.Member;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;

public class CopycatClusterRule<S extends StateMachine> implements TestRule {
	public static final Logger logger = LoggerFactory.getLogger(CopycatClusterRule.class);
	
	public static Supplier<Storage> DEFAULT_STORAGE_SUPPLIER = ()->{
	  return Storage.builder()
      .withStorageLevel(StorageLevel.MEMORY)
      .withMaxSegmentSize(1024 * 1024)
      .withCompactionThreads(1)
      .build();
	};
	
	public static Supplier<Supplier<Transport>> DEFAULT_TRANSPORT_SUPPLIER = ()->{
	  LocalServerRegistry registry = new LocalServerRegistry();
	  return ()->{
	    return new LocalTransport(registry);
	  };
	};
	
	public static class Builder<S extends StateMachine> {
	private int serverCount = 3;
	private int initialPort = 5000;
	private Supplier<S> stateMachineSupplier;
    private Optional<Supplier<Storage>> storageSupplier = Optional.empty();
    private Optional<Supplier<Transport>> transportSupplier = Optional.empty();
    private Optional<Supplier<Serializer>> serializer = Optional.empty();
    
	public Builder<S> withServerCount( int serverCount ) {
		this.serverCount = serverCount;
		return this;
	}
	
	public Builder<S> withInitalPort( int initialPort ) {
		this.initialPort = initialPort;
		return this;
	}
	
	public Builder<S> withStateMachineSupplier( Supplier<S> stateMachineSupplier ) {
	  this.stateMachineSupplier = stateMachineSupplier;
	  return this;
	}
	
	public Builder<S> with( Consumer<Builder<S>> configurator ) {
	  configurator.accept(this);
	  return this;
	}
	
	  public CopycatClusterRule<S> build() {
	    return new CopycatClusterRule<S>(
	        serverCount,
	        initialPort,
	        stateMachineSupplier,
	        storageSupplier.orElse(DEFAULT_STORAGE_SUPPLIER),
	        transportSupplier.orElse(DEFAULT_TRANSPORT_SUPPLIER.get()),
	        serializer.orElse(Serializer::new));
	  }

    public Builder<S> withStorageSupplier(Supplier<Storage> storageSupplier) {
      this.storageSupplier = Optional.ofNullable(storageSupplier);
      return this;
    }
    
    public Builder<S> withDirectoryStorage(String directory) {
      try {
        File storageDir = new File(directory);
        storageDir.mkdirs();
        FileUtils.cleanDirectory(storageDir);
        AtomicInteger index = new AtomicInteger(0);
        return withStorageSupplier(()->{
          File replicaStorageDir = new File(storageDir, "server_"+index.getAndIncrement());
          replicaStorageDir.mkdir();
          try {
              FileUtils.cleanDirectory(replicaStorageDir);
          } catch (Exception e) {
              throw new RuntimeException(e);
          }
          return new Storage(replicaStorageDir);
      });
        } catch ( IOException ioe ) {
          throw new RuntimeException(ioe);
        }
    }

    public Builder<S> withTransportSupplier(Supplier<Transport> transportSupplier) {
      this.transportSupplier = Optional.ofNullable(transportSupplier);
      return this;
    }

    public Builder<S> withSerializer(Supplier<Serializer> serializer) {
      this.serializer = Optional.ofNullable(serializer);
      return this;
    }
	}
	
	public static class TestMember implements Member, Serializable {
    private static final long serialVersionUID = 1L;
    private Type type;
    private Address serverAddress;
    private Address clientAddress;
  
    public TestMember() {
    }
  
    public TestMember(Type type, Address serverAddress, Address clientAddress) {
      this.type = type;
      this.serverAddress = serverAddress;
      this.clientAddress = clientAddress;
    }
  
    @Override
    public int id() {
      return serverAddress.hashCode();
    }
  
    @Override
    public Address address() {
      return serverAddress;
    }
  
    @Override
    public Address clientAddress() {
      return clientAddress;
    }
  
    @Override
    public Address serverAddress() {
      return serverAddress;
    }
  
    @Override
    public Type type() {
      return type;
    }
  
    @Override
    public Listener<Type> onTypeChange(Consumer<Type> callback) {
      return null;
    }
  
    @Override
    public Status status() {
      return null;
    }
  
    @Override
    public Instant updated() {
      return null;
    }
  
    @Override
    public Listener<Status> onStatusChange(Consumer<Status> callback) {
      return null;
    }
  
    @Override
    public CompletableFuture<Void> promote() {
      return null;
    }
  
    @Override
    public CompletableFuture<Void> promote(Type type) {
      return null;
    }
  
    @Override
    public CompletableFuture<Void> demote() {
      return null;
    }
  
    @Override
    public CompletableFuture<Void> demote(Type type) {
      return null;
    }
  
    @Override
    public CompletableFuture<Void> remove() {
      return null;
    }
  }

  public static <S extends StateMachine> Builder<S> builder() {
	  return new Builder<S>();
	}
	
	private Supplier<S> stateMachineSupplier;
	private int initialPort;
	private int serverCount;
    private Supplier<Storage> storageSupplier;
    private Supplier<Transport> transportSupplier;
    private Supplier<Serializer> serializerSupplier;
	
	public CopycatClusterRule(int serverCount, int initialPort, Supplier<S> stateMachineSupplier, Supplier<Storage> storageSupplier, Supplier<Transport> transportSupplier, Supplier<Serializer> serializerSupplier ) {
	  this.initialPort = initialPort;
	  this.stateMachineSupplier = stateMachineSupplier;
	  this.storageSupplier = storageSupplier;
	  this.transportSupplier = transportSupplier;
	  this.serializerSupplier = serializerSupplier;
	  this.serverCount = serverCount;
	}
	
	private int port;
	private List<Member> members = Lists.newArrayList();
	private List<CopycatServer> servers = Lists.newArrayList();
	private List<CopycatClient> clients = Lists.newArrayList();
	
    public List<Member> getMembers() {
      return members;
    }
    
    public List<CopycatServer> getServers() {
      return servers;
    }
	
	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
				    port = initialPort;
				    
				    createServers(serverCount);

					base.evaluate();
				}
				finally {
				    clients.forEach(c -> {
				      try {
				        c.close().join();
				      } catch (Exception e) {
				      }
				    });
				    
				    servers.forEach(s -> {
				      try {
				        if (s.isRunning()) {
				          s.kill().join();
				          s.delete().join();
				        }
				      } catch (Exception e) {
				      }
				    });

				    members.clear();
				    port = 5000;
                    clients.clear();
				    servers.clear();
				}
			}
		};
	}
	
	  /**
	   * Creates a set of Copycat servers.
	   */
	  private List<CopycatServer> createServers(int nodes) throws Throwable {
	    CountDownLatch serverLatch = new CountDownLatch(nodes);

	    for (int i = 0; i < nodes; i++) {
	      members.add(nextMember(Member.Type.INACTIVE));
	    }

	    for (int i = 0; i < nodes; i++) {
	      CopycatServer server = createServer(members, members.get(i));
	      server.start().thenRun(serverLatch::countDown);
	    }

	    serverLatch.await(30*nodes, TimeUnit.SECONDS);

	    return servers;
	  }
	  
	  
	  private Member nextMember(Member.Type type) {
	    return new CopycatClusterRule.TestMember(type, new Address("localhost", ++port), new Address("localhost", port + 1000));
	  }
	  
	  private CopycatServer createServer(List<Member> members, Member member) {
	    @SuppressWarnings("unchecked")
      CopycatServer.Builder builder = CopycatServer.builder(member.clientAddress(), member.serverAddress(), members.stream().map(Member::serverAddress).collect(Collectors.toList()))
	      .withTransport(transportSupplier.get())
	      .withStorage(storageSupplier.get())
	      .withSerializer(serializerSupplier.get())
	      .withStateMachine((Supplier<StateMachine>)stateMachineSupplier);

	    if (member.type() != Member.Type.INACTIVE) {
	      builder.withType(member.type());
	    }

	    CopycatServer server = builder.build();
	    server.serializer().disableWhitelist();
	    servers.add(server);
	    return server;
	  }
	  

	  public CopycatClient createClient() throws InterruptedException {
	    CopycatClient client = CopycatClient.builder(members.stream().map(Member::clientAddress).collect(Collectors.toList()))
	        .withTransport(transportSupplier.get())
	        .withSerializer(serializerSupplier.get())
	        .withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
	        .withRetryStrategy(RetryStrategies.FIBONACCI_BACKOFF)
	        .build();
	      client.serializer().disableWhitelist();
	      CountDownLatch latch = new CountDownLatch(1);
	      client.connect().thenRun(latch::countDown);
	      latch.await(10, TimeUnit.SECONDS);
	      clients.add(client);
	      return client;
	  }
}
