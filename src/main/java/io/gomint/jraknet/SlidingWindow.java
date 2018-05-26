package io.gomint.jraknet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author geNAZt
 * @version 1.0
 */
public class SlidingWindow {

    private static final Logger LOGGER = LoggerFactory.getLogger( SlidingWindow.class );
    private static final int MAX_THRESHOLD = 2000;
    private static final int ADDITIONAL_VARIANCE = 30;

    private final int maxMTU;
    private int threshold;
    private int cwnd;

    private double lastRTT;
    private double estimatedRTT = -1;
    private double deviationRTT = -1;

    private boolean ackAlreadyTicked;
    private boolean nakAlreadyTicked;
    private boolean resendAlreadyTicked;

    SlidingWindow( int maxMTU ) {
        this.maxMTU = maxMTU;
        this.cwnd = this.maxMTU;
    }

    public int getTransmissionBandwidth( int unackedBytes ) {
        if ( unackedBytes <= this.cwnd ) {
            return this.cwnd - unackedBytes;
        }

        return 0;
    }

    public void onNAK() {
        if ( this.nakAlreadyTicked ) {
            return;
        }

        this.nakAlreadyTicked = true;
        this.threshold = this.cwnd / 2;
    }

    public void onACK( double rtt ) {
        if ( this.ackAlreadyTicked ) {
            return;
        }

        this.ackAlreadyTicked = true;


        this.lastRTT = rtt;
        if ( this.estimatedRTT == -1 ) {
            this.estimatedRTT = rtt;
            this.deviationRTT = rtt;
        } else {
            double d = .05;
            double difference = rtt - this.estimatedRTT;
            this.estimatedRTT = this.estimatedRTT + d * difference;
            this.deviationRTT = this.deviationRTT + d * ( Math.abs( difference ) - this.deviationRTT );
        }

        if ( this.isInSlowStart() ) {
            this.cwnd += this.maxMTU;
            if ( this.cwnd > this.threshold && this.threshold != 0 ) {
                this.cwnd = this.threshold + this.maxMTU * this.maxMTU / this.cwnd;
            }

            LOGGER.debug( "Slow start bandwidth increase: {} / {}", this.cwnd, this.threshold );
        } else {
            this.cwnd += this.maxMTU * this.maxMTU / this.cwnd;
            LOGGER.debug( "Bandwidth increase: {}", this.cwnd );
        }
    }

    public long getRTOForRetransmission( int timesSent ) {
        if ( this.estimatedRTT == -1 ) {
            return MAX_THRESHOLD;
        }

        double u = 2.0;
        double q = 4.0;

        long threshhold = (long) ( u * this.estimatedRTT + q * this.deviationRTT ) + ADDITIONAL_VARIANCE;
        if ( threshhold > MAX_THRESHOLD ) {
            return MAX_THRESHOLD;
        }

        LOGGER.debug( "Resending with " + threshhold + " ms delay" );
        return threshhold;
    }

    public void onTickFinish() {
        this.ackAlreadyTicked = false;
        this.nakAlreadyTicked = false;
        this.resendAlreadyTicked = false;
    }

    private boolean isInSlowStart() {
        return this.cwnd <= this.threshold || this.threshold == 0;
    }

    public void onResend() {
        if ( this.resendAlreadyTicked ) {
            return;
        }

        this.resendAlreadyTicked = true;
        if ( this.cwnd > this.maxMTU * 2 ) {
            this.threshold = this.cwnd / 2;
            if ( this.threshold < this.maxMTU ) {
                this.threshold = this.maxMTU;
            }

            this.cwnd = this.maxMTU;
        }
    }

    public int getReTransmissionBandwidth( int unackedBytes ) {
        return unackedBytes; // Allow to resend all bytes in one go
    }

}
