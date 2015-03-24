package jp.ohwada.android.bluetoothchattester;

/**
 * SequenceChecker
 */
public class SequenceChecker {

    private static final String LF = "\n";
    private static final int NUM = 10;

    private int[] mArray = new int[NUM];
    private int[] mExpects = new int[NUM];
    private int[] mErrors = new int[NUM];
    private int mTotal = 0;
    private boolean isStart = false;

    /**
     * SequenceChecker
     */
    public SequenceChecker() {
        // dummy
    }

    /**
     * init
     */
    public void init() {
        isStart = false;
        mTotal = 0;
        for ( int k=0; k<NUM; k++ ) {
            mExpects[k] = 0;
            mErrors[k]  = 0;
        }
    }

    /**
     * check
     */
    public boolean check( int value ) {
        // shift 
        for (int i = NUM - 1; i > 0; i-- ) {  
            mArray[ i ] = mArray[ i - 1 ];
        } 
        mArray[ 0 ] = value;
        // init matches
        boolean[] matches = new boolean[NUM];
        for (int k=0; k<NUM; k++ ) {  
            matches[k] = true;
        }
        // check sequence 1,2,3,...
        int n = mArray[0];
        for (int i=1; i<NUM; i++ ) { 
            boolean match = false; 
            if ( mArray[i] == n - i ) { 
               match = true; 
            }
            // k is the length of the sequence to be checked
            // For example
            // if k is 5, check whether sequence are 1,2,3,4,5
            for ( int k=0; k<NUM; k++ ) {
                // check value
                if (( k > i - 1 )&& !match ) { 
                    matches[k] = false;
                }
            }
        }
        // unless match once, 
        if ( !isStart ) {
            // if sequence are matched, 
            // count the error from the next time
            if ( matches[ NUM - 1 ] ) {
                isStart = true;
                for ( int k=0; k<NUM; k++ ) {
                    mExpects[k] = n + 1;
                }
            }
            return true;
        }
        boolean ret = true;
        // count total
        mTotal ++;
        // count the error
        // and set next expected value
        for ( int k=0; k<NUM; k++ ) {
            boolean is_ok = false;
            // same as the expected value
            if ( n == mExpects[k] ) {
                mExpects[k] ++;
                if ( matches[k] ) {
                    is_ok = true;
                }
            // larger than expected value
            } else if ( n > mExpects[k] ) {
                if ( matches[k] ) {
                    mExpects[k] = n + 1;
                } 
            // less than expected value
            }
            if ( !is_ok ) {
                mErrors[k] ++;
                if ( k == 2 ) {
                    ret = false;
                }
            }
        }
        return ret;
    }

    /**
     * getResult
     */
    public String getResult() {
        String str = "";
        float total = (float)mTotal;
        str += "total " + mTotal + LF;
        for ( int k=2; k<NUM; k++ ) {
            float p1 = 100 * (float)mErrors[k] / total;
            float p2 = p1 / (float)( k + 1 );
            String mark = "";
            if ( p1 > 80 ) {
                mark = "* ";
            }
            str += "Error " + k + " : " + mark + mErrors[k] + " " + Float.toString( p2 ) + " " + Float.toString( p1 ) + LF;
        }
        return str;
    }
   
} 
 