package jp.ohwada.android.bluetoothchattester;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.os.SystemClock;
import android.util.Log;

/**
 *  String Utility
 */
public class StringUtility {
    /** Debug */
    private static final boolean D = true;
    private static final boolean D_READ = false;
    private static final String TAG = "StringUtility";

    // char
    private static final byte LF = 0x0a;
    private static final String CHARSET_NAME = "UTF-8";
    
    // param
    private int mInterval = 100;
    private int mTimeout = 500;   
    private int mMaxSize = 256;

    // recv
    private String mPrevBuf = "";
    private long mPrevTime = 0;

    /**
     * === constructor ===
     */	
    public StringUtility() {
        mPrevBuf = "";
        mPrevTime = System.currentTimeMillis();
    } 

    /**
     * setInterval
     */	
    public void setInterval( int n ) {
        mInterval = n;
    }

    /**
     * setTimeout
     */
    public void setTimeout( int n ) {
        mTimeout = n;
    }

    /**
     * setMaxSize
     */
    public void setMaxSize( int n ) {
        mMaxSize = n;
    }

    /**
     * getListString
     * @param byte[] buffer
     */
    public List<String> getListString( byte[] bytes ) {
        List<String> list = new ArrayList<String>();
        if ( bytes == null ) return list;
        int length = bytes.length;
        if ( length == 0 ) return list;
        if ( D_READ ) {
            log_bytes( bytes );
        }
        int offset = 0;
        String str = "";
        String buf = "";
        boolean is_prev = true;
        long time = SystemClock.elapsedRealtime() - mPrevTime;
        if ( time > mInterval ) {
            if ( mPrevBuf.length() > 0 ) {
                list.add( mPrevBuf );
                log_d( "too long interval " + time );
            }
            is_prev = false;
            mPrevBuf = "";            
        }
        // search LF
        for ( int i=0; i<length; i++ ) {
            if ( bytes[i] == LF ) {
                // 
                if ( i == offset ) {
                    // set empty, if no valid byte
                    str = "";
                } else {
                    // convert string
                    str = bytesToString( bytes, offset, i - offset );
                }
                // if remain the prevous string, add this.
                if ( is_prev == true ) {
                    str = mPrevBuf + str;
                    is_prev = false;
                    mPrevBuf = "";
                }
                // set in list
                list.add( str );
                // set next offset
                offset = i + 1;
            }
        }
        if ( length == offset ) {
            // set empty, if search at end
            buf = "";
        } else {
            // convert string
            buf = bytesToString( bytes, offset, length - offset );
        }
        if ( is_prev == true ) {
            // if remain the prevous string, add this.
            mPrevBuf += buf;
        } else {
            // set new buf
            mPrevBuf = buf;
        }
        // clear recv buffer, if buffer size is over limit
        if ( mPrevBuf.length() > mMaxSize ) {
            mPrevBuf = "";
            log_d( "buffer overflow" );
        }
        mPrevTime = SystemClock.elapsedRealtime();
        return list;
    }

    /**
     * bytesToString
     */
    private String bytesToString( byte[] bytes, int offset, int count ) {
        String str= "";
            try {
                str = new String( bytes, offset, count, CHARSET_NAME );
                str = str.trim();
            } catch ( UnsupportedEncodingException e) {
                if (D) e.printStackTrace();
            }
    	return str;
    }

    /**
     * getListWhenTimeout
     */
    public List<String> getListWhenTimeout() {
        List<String> list = new ArrayList<String>();
        long time = SystemClock.elapsedRealtime() - mPrevTime;
        if (( time > mTimeout )&&( mPrevBuf.length() > 0 )) { 
            log_d( "timeout " + time );
            list.add( mPrevBuf );
            mPrevBuf = "" ;
        }
        return list;    
    }

// --- debug log ---
    /**
     * log_bytes
     * @param String str
     * @param byte[] bytes
     */
    private void log_bytes( byte[] bytes ) {
        String msg = "";
        for ( int i=0; i<bytes.length ; i++ ) {
            msg += String.format( "%02X", bytes[ i ] );
            msg += " ";
        }
        log_d( msg );
    }

    /**
     * write log
     * @param String msg
     */ 
    private void log_d( String msg ) {
        if(D) Log.d(TAG, msg);
    }   
}
