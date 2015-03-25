package jp.ohwada.android.bluetoothchattester;

import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MainActivity extends BluetoothChat {

    // Debugging
    private static final String TAG2 = "MainActivity";
    private static final boolean D2 = true;
    private static final boolean D3 = true;

    // timer
    private static final int TIMER_DELAY = 1;   // 1ms
    private static final int TIMER_WHAT = 1;

    // mode
    private static final int MODE_ECHO = 0;
    private static final int MODE_TESTER = 1;

    // test param
    private static final int MAX_COUNT = 1100;

    // char
    private static final String LF =  "\n";
    
    // class object
    private StringUtility mStringUtility;
    private SequenceChecker mSequenceChecker;

    // UI
    private Button mButtonStart;
    private Button mButtonStop;
    private Button mButtonReset;
    private ListView mListViewRecv;
    private ArrayAdapter<String> mRecvArrayAdapter;

    // mode
    private int mMode = MODE_ECHO;

    // send param
    private int mCount = 0;
    private boolean isStart = false;
    private long mStartTime = 0;

    // timer
    private boolean isTimerStart;
    private boolean isTimerRunning;

    /**
     * onCreate
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {      
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main );

        mSequenceChecker = new SequenceChecker();
        mStringUtility = new StringUtility();
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    /**
     * setupChat (override)
     */
    protected void setupChat() {
        RadioGroup radioGroupMode = (RadioGroup) findViewById( R.id.RadioGroup_mode );
        radioGroupMode.check( R.id.RadioButton_mode_echo );
        radioGroupMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) { 
                execCheckedChanged( group, checkedId );
            }
        });

        mButtonStart = (Button) findViewById( R.id.Button_start );
        mButtonStart.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                execStart();
            }
        });

        mButtonStop = (Button) findViewById( R.id.Button_stop );
        mButtonStop.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                execStop();
            }
        });

        mButtonReset = (Button) findViewById( R.id.Button_reset );
        mButtonReset.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                execReset();
            }
        });

        mMode = MODE_ECHO;
        mButtonStart.setVisibility( View.INVISIBLE );
        mButtonStop.setVisibility( View.INVISIBLE );

        // Initialize the array adapter for the conversation thread
        mRecvArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListViewRecv = (ListView) findViewById( R.id.ListView_recv );
        mListViewRecv.setAdapter( mRecvArrayAdapter );

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
    }

    /**
     * execCheckedChanged
     */
    private void execCheckedChanged( RadioGroup group, int checkedId ) { 
        RadioButton radioButton = (RadioButton) findViewById(checkedId);
        int id = radioButton.getId();
        if ( id == R.id.RadioButton_mode_echo ) {
            mMode = MODE_ECHO;
            mButtonStart.setVisibility( View.INVISIBLE );
            mButtonStop.setVisibility( View.INVISIBLE );
        } else if ( id == R.id.RadioButton_mode_tester ) {
            mMode = MODE_TESTER;
            mButtonStart.setVisibility( View.VISIBLE );
            mButtonStop.setVisibility( View.VISIBLE );
        }
    }

    /**
     * onDestroy (override)
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage2(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            write( send );
        }
    }

    /**
     * write
     */
    private void write( byte[] message ) {
        mChatService.write( message );
    }

    /**
     * execRead (override)
     * @param Message msg
     */
    protected void execRead( Message msg ) {
        log_d( "execRead" );
        // create valid data bytes 
        byte[] buffer = (byte[]) msg.obj;
        int length = (int) msg.arg1;
        byte[] bytes = new byte[ length ];
        for ( int i=0; i<length; i++ ) {
            bytes[ i ] = buffer[ i ];
        }
        if ( mMode == MODE_ECHO ) {
            write( bytes );
        }
        List<String> list = mStringUtility.getListString( bytes );
        if ( list.size() > 0 ) {
            execRead( list );
        }
    }

    /**
     * execRead
     * @param List<String> list
     */
    private void execRead( List<String> list ) {
        for ( String str: list ) {
            if ( str.length() == 0 ) continue;
            int n = parseInt( str );
            boolean flag = mSequenceChecker.check( n );
            String mark = " ";
            if ( !flag ) {
                mark = "*";
            } 
            String msg = mark +  " " + str;
            if (D3) mRecvArrayAdapter.add( msg );
            log_d( msg );
            if ( n > MAX_COUNT ) {
                execStop();
            }
        }
    }

    /**
     * parseInt
     * @param String str
     * @return int
     */
    private int parseInt( String str ) {
        int n = 0;
        try {
            n = Integer.parseInt( str );
        } catch ( Exception e ) {
           e.printStackTrace();
        }
        return n;
    } 
    
// --- comand ---
     /**
     * execStart
     */
    private void execStart() {
        isStart = true;
        mStartTime = SystemClock.elapsedRealtime();
        execReset();
        startTimer();
    }

    /**
     * execStop
     */
    private void execStop() {
        stopTimer();
        if ( isStart ) {
            isStart = false;
            long time = SystemClock.elapsedRealtime() - mStartTime;
            String msg = "time " + time + LF;
            msg += mSequenceChecker.getResult();
            mRecvArrayAdapter.add( msg );
            log_d( msg );
        }
    }

    /**
     * execReset
     */
    private void execReset() {
        mCount = 0;
        mSequenceChecker.init();
        mRecvArrayAdapter.clear();
    }

    /**
     * startTimer
     */
    private void startTimer() {
        if ( mMode != MODE_TESTER ) return;
        isTimerStart = true;
        updateTimerRunning();
    }

    /**
     * stopTimer
     */	
    private void stopTimer() {
        if ( mMode != MODE_TESTER ) return;
        isTimerStart = false;
        updateTimerRunning();
    }

    /**
     * updateTimerRunning
     */	
    private void updateTimerRunning() {
        boolean running = isTimerStart;
        if (running != isTimerRunning) {
            if (running) {
                updateSend();
                timerHandler.sendMessageDelayed(
                    Message.obtain(timerHandler, TIMER_WHAT), TIMER_DELAY );               
             } else {
                timerHandler.removeMessages(TIMER_WHAT);
            }
            isTimerRunning = running;
        }
    }

    /**
     * --- timerHandler ---
     */	    
    private Handler timerHandler = new Handler() {
        public void handleMessage(Message m) {
            if (isTimerRunning) {
                updateSend();
                sendMessageDelayed(
                    Message.obtain(this, TIMER_WHAT), TIMER_DELAY);
            }
        }
    };

    /**
     * updateSend
     */	
    private synchronized void updateSend() {
        sendMessage2( Integer.toString( mCount ) + LF );
        mCount ++;
    }

    /**
     * write log
     * @param String msg
     */ 
    private void log_d( String msg ) {
        if(D2) Log.d(TAG2, msg);
    }
}