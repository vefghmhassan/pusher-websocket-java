package com.pusher.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.ChannelManager;
import com.pusher.client.channel.PublicChannel;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.InternalConnection;
import com.pusher.client.util.Factory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Factory.class})
public class PusherTest {

    private static final String API_KEY = "123456";
    private static final String PUBLIC_CHANNEL_NAME = "my-channel";
    
    private Pusher pusher;
    private @Mock InternalConnection mockConnection;
    private @Mock ChannelManager mockChannelManager;
    private @Mock ConnectionEventListener mockConnectionEventListener;
    private @Mock PublicChannel mockPublicChannel;
    private @Mock ChannelEventListener mockChannelEventListener;
    
    @Before
    public void setUp()
    {
	PowerMockito.mockStatic(Factory.class);
	when(Factory.getConnection(API_KEY)).thenReturn(mockConnection);
	when(Factory.getChannelManager(mockConnection)).thenReturn(mockChannelManager);
	when(Factory.newPublicChannel(PUBLIC_CHANNEL_NAME)).thenReturn(mockPublicChannel);
	
	this.pusher = new Pusher(API_KEY);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullAPIKeyThrowsIllegalArgumentException() {
	new Pusher(null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyAPIKeyThrowsIllegalArgumentException() {
	new Pusher("");
    }
    
    @Test
    public void testCreatesConnectionObjectWhenConstructed() {
	assertNotNull(pusher.getConnection());
	assertSame(mockConnection, pusher.getConnection());
    }
    
    @Test
    public void testConnectCallWithNoListenerIsDelegatedToUnderlyingConnection() {
	pusher.connect();
	verify(mockConnection).connect();
    }
    
    @Test
    public void testConnectCallWithListenerAndEventsBindsListenerToEventsBeforeConnecting() {
	pusher.connect(mockConnectionEventListener, ConnectionState.CONNECTED, ConnectionState.DISCONNECTED);
	
	verify(mockConnection).bind(ConnectionState.CONNECTED, mockConnectionEventListener);
	verify(mockConnection).bind(ConnectionState.DISCONNECTED, mockConnectionEventListener);
	verify(mockConnection).connect();
    }
    
    @Test
    public void testConnectCallWithListenerAndNoEventsBindsListenerToAllEventsBeforeConnecting() {
	pusher.connect(mockConnectionEventListener);
	
	verify(mockConnection).bind(ConnectionState.ALL, mockConnectionEventListener);
	verify(mockConnection).connect();
    }
    
    @Test
    public void testConnectCallWithNullListenerAndNoEventsJustConnectsWithoutBinding() {
	pusher.connect(null);
	
	verify(mockConnection, never()).bind(any(ConnectionState.class), any(ConnectionEventListener.class));
	verify(mockConnection).connect();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConnectCallWithNullListenerAndEventsThrowsException() {
	pusher.connect(null, ConnectionState.CONNECTED);
    }
    
    @Test
    public void testSubscribeCreatesPublicChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener);
	
	verify(mockChannelManager).subscribeTo(mockPublicChannel, mockChannelEventListener);
    }
    
    @Test
    public void testSubscribeWithEventNamesCreatesPublicChannelAndDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener, "event1", "event2");
	
	verify(mockChannelManager).subscribeTo(mockPublicChannel, mockChannelEventListener, "event1", "event2");
    }

    @Test(expected=IllegalStateException.class)
    public void testSubscribeWhenConnectingThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
	
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testSubscribeWhenDisconnectedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
	
	pusher.subscribe(PUBLIC_CHANNEL_NAME, mockChannelEventListener);
    }
    
    @Test
    public void testUnsubscribeDelegatesCallToTheChannelManager() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTED);
	pusher.unsubscribe(PUBLIC_CHANNEL_NAME);
	verify(mockChannelManager).unsubscribeFrom(PUBLIC_CHANNEL_NAME);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testUnsubscribeWhenDisconnectedThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.DISCONNECTED);
	
	pusher.unsubscribe(PUBLIC_CHANNEL_NAME);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testUnsubscribeWhenConnectingThrowsException() {
	when(mockConnection.getState()).thenReturn(ConnectionState.CONNECTING);
	
	pusher.unsubscribe(PUBLIC_CHANNEL_NAME);
    }
}