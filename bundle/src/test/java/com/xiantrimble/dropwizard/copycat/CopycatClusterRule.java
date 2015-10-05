package com.xiantrimble.dropwizard.copycat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.catalyst.util.Listener;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.RaftServer;
import io.atomix.copycat.server.RaftServer.State;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.storage.Storage;

public class CopycatClusterRule<S extends StateMachine> implements TestRule {
	public static final Logger logger = LoggerFactory.getLogger(CopycatClusterRule.class);
	private int serverCount;
	private int clientCount;
	private String host = "localhost";
	private int initialPort;
	private long timeout = 1;
	private TimeUnit timeoutUnit = TimeUnit.MINUTES;
	private String storagePath = "target/copycat/storage";
	private Supplier<S> stateMachineSupplier;
	private List<Listener<State>> stateChangeListeners = new ArrayList<>();
	public CopycatClusterRule<S> withServerCount( int serverCount ) {
		this.serverCount = serverCount;
		return this;
	}
	
	public CopycatClusterRule<S> withClientCount( int clientCount ) {
		this.clientCount = clientCount;
		return this;
	}
	
	public CopycatClusterRule<S> withInitalPort( int initialPort ) {
		this.initialPort = initialPort;
		return this;
	}
	
	public CopycatClusterRule<S> withStoragePath( String storagePath ) {
		this.storagePath = storagePath;
		return this;
	}
	
	public CopycatClusterRule<S> withTimeout( long timeout, TimeUnit timeoutUnit ) {
		this.timeout = timeout;
		this.timeoutUnit = timeoutUnit;
		return this;
	}
	
	public CopycatClusterRule<S> withStateMachine( Supplier<S> stateMachineSupplier ) {
		this.stateMachineSupplier = stateMachineSupplier;
		return this;
	}
	
	private List<CopycatServer> servers = Lists.newArrayList();
	private List<Address> addressList;
	private List<File> storageDirList;
	private List<CopycatClient> clients = Lists.newArrayList();
	
	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					long endTime = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);
					
					File storageDir = new File(storagePath);
					storageDir.mkdirs();
					FileUtils.cleanDirectory(storageDir);
					
					storageDirList = IntStream.range(0, serverCount)
							.mapToObj(CopycatClusterRule.this::storageDirForIndex)
							.collect(Collectors.toList());
					
				addressList = 
						IntStream.range(0, serverCount)
						.mapToObj(CopycatClusterRule.this::addressForIndex)
						.collect(Collectors.toList());
				
				servers = IntStream.range(0, serverCount)
						.mapToObj(CopycatClusterRule.this::serverForIndex)
						.collect(Collectors.toList());
				
				clients = IntStream.range(0, clientCount)
						.mapToObj((index)->CopycatClient.builder(addressList)
								.withTransport(new NettyTransport(5)).build())
						.collect(Collectors.toList());
				
				// start the cluster.
				CopyOnWriteArrayList<Throwable> failures = Lists.newCopyOnWriteArrayList();
				CountDownLatch serverLatch = new CountDownLatch(serverCount);
				servers.stream()
				  .forEach(c->{
					  c.open().whenCompleteAsync((c2, e)->{
						  if( e != null ) failures.add(e);
						  serverLatch.countDown();
					  });
				  });
                serverLatch.await(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                
                if( !failures.isEmpty() ) {
                	throw new RuntimeException("failed to start servers", failures.get(0));
                }
                
				CountDownLatch clientLatch = new CountDownLatch(clientCount);
				clients.stream()
				  .forEach(c->{
					  c.open().whenCompleteAsync((c2, e)->{
						  if( e != null ) failures.add(e);
						  clientLatch.countDown();
					  });
				  });
                clientLatch.await(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);

        		leaderRef.set(servers.stream()
        				  .filter(CopycatServer::isOpen)
        				  .filter(server->server.state()==State.LEADER)
        				  .findFirst()
        				  .orElseThrow(RuntimeException::new));
        		
				stateChangeListeners = servers.stream()
						.map(s->s.onStateChange(stateChange(s)))
						.collect(Collectors.toList());
                
                if( !failures.isEmpty() ) {
                	throw new RuntimeException("failed to start clients", failures.get(0));
                }

				base.evaluate();
				}
				finally {
					long endTime = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);
					stateChangeListeners.forEach(l->l.close());
					CopyOnWriteArrayList<Throwable> failures = Lists.newCopyOnWriteArrayList();
					CountDownLatch latch = new CountDownLatch(serverCount+clientCount);
					Stream.concat(servers.stream(), clients.stream())
					  .forEach(c->{
						  c.close().whenCompleteAsync((c2, e)->{
							  if( e != null ) failures.add(e);
							  latch.countDown();
						  });
					  });
	                latch.await(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);	
				}
			}
		};
	}

	public CopycatServer getServer(int index) {
		return servers.get(index);
	}

	public CopycatClient getClient(int index) {
		return clients.get(index);
	}
	
	public Address addressForIndex( int index ) {
		return new Address(host, initialPort+index);
	}
	
	public File storageDirForIndex( int index ) {
		File storageDir = new File(storagePath);
		File replicaStorageDir = new File(storageDir, "server_"+serverCount);
		replicaStorageDir.mkdir();
		try {
			FileUtils.cleanDirectory(replicaStorageDir);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return replicaStorageDir;
	}
	
	public CopycatServer serverForIndex(int index) {
		return CopycatServer.builder(addressList.get(index), addressList)
		.withStorage(new Storage(storageDirList.get(index)))
		.withStateMachine(stateMachineSupplier.get())
		.withTransport(new NettyTransport(5))
		.build();
	}

	public RaftServer addServer() throws InterruptedException, ExecutionException {
		storageDirList.add(storageDirForIndex(serverCount));
		addressList.add(addressForIndex(serverCount));
		CopycatServer server = serverForIndex(serverCount);
		servers.add(server);
		serverCount++;
		return server.open().get();
	}
	
	public AtomicReference<CopycatServer> leaderRef = new AtomicReference<>();
	
	Consumer<State> stateChange( CopycatServer s ) {
		return (state)->{
			logger.warn("State changed {}", s.toString());
			if( state == State.LEADER ) leaderRef.set(s);
		};
	}

	public void stopLeader(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, RuntimeException {
		CountDownLatch leaderChange = new CountDownLatch(1);
		
		CopycatServer currentLeader = leaderRef.get();
		
		List<Listener<State>> stateChangeListeners = servers.stream().map(s->s.onStateChange(state->{
			logger.warn("LEADER CHANGE!");
			if( state == State.LEADER && !s.equals(currentLeader)) leaderChange.countDown();
	    }
		)).collect(Collectors.toList());getClass();
		
		leaderRef.get().close().get();
		
		try {
			if( !leaderChange.await(timeout, timeUnit) ) {
				throw new RuntimeException("New leader not chosen.");
			}
		}
		finally {
			stateChangeListeners.forEach(l->l.close());
		}
	}
	
	public Optional<Address> getLeader() {
		CopycatServer leader = leaderRef.get();
		if( leader == null ) return Optional.empty();
		return Optional.ofNullable(leader
				.leader());
	}

}
