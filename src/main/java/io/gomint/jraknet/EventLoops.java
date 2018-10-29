package io.gomint.jraknet;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author geNAZt
 * @version 1.0
 */
public class EventLoops {

    public static ScheduledExecutorService TICKER;
    public static EventLoopGroup LOOP_GROUP;
    public static Flusher FLUSHER;

    static {
        EventLoops.TICKER = Executors.newScheduledThreadPool( 2, new ThreadFactory() {
            @Override
            public Thread newThread( Runnable r ) {
                return new Thread( r, "jRaknet Ticker" );
            }
        } );
        EventLoops.LOOP_GROUP = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
        EventLoops.FLUSHER = new Flusher( EventLoops.LOOP_GROUP.next() );
    }

    public static void cleanup() {
        EventLoops.TICKER.shutdownNow();

        try {
            EventLoops.LOOP_GROUP.shutdownGracefully().await();
        } catch ( InterruptedException e ) {
            // Ignore
        }
    }

}
