import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.util.Arrays;

public class WakuwakuSampler implements Runnable {
	AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
	File wavFile;
	
	static final float SAMPLE_RATE = 44100;
	static final int SAMPLE_SIZE_IN_BITS = 16;
	static final int CHANNELS = 1;
	static final boolean SIGNED = true;
	static final boolean BIG_ENDIAN = true;
	
	static int lowTh = 200;
	static int HighTh = 1500;
	
	TargetDataLine line;
	String fileName;
	AudioFormat format;

	double[] valuesActual, valuesImaginal;

	public static void main(String[] args) throws Exception {
		new WakuwakuSampler();
	}
	
	public WakuwakuSampler() throws Exception {
		format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        line = AudioSystem.getTargetDataLine(format);
        line.open(format);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println("Press Enter to START Recording...");
        in.readLine();
        Thread thread = new Thread(this);
        thread.start();
         
        System.out.println("Press Enter to STOP Recording...");
        in.readLine();
        stopRecording();
        
        
        initialize();
        dft(2048);
        
        
	}
	
	public void startRecording() {
		try {
			System.err.println(fileName);
			File wavFile = new File(fileName);
		    line.start();
		    AudioInputStream ais = new AudioInputStream(line);
		    AudioSystem.write(ais, fileType, wavFile);
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		}
    }
	
	public void stopRecording() {
	    line.stop();
		line.close();
	} 
	
	public void run() {
		fileName = "waku" + System.currentTimeMillis() + ".wav";
        startRecording();
	}
	
	void dft(int windowSize) {
		for (int i = 0; i < valuesActual.length; i += windowSize) {
			if (valuesActual.length - i >= windowSize) { 
				double[] spectrumActual = new double[ windowSize ];
				double[] spectrumImaginal = new double[ windowSize ];
				//System.out.println("### i: "+i+", array size: "+valuesActual.length);
				double[] valuesInWindow = Arrays.copyOfRange(valuesActual, i, i+windowSize);
				
				dft( valuesInWindow , spectrumActual , spectrumImaginal , false );
				double binHz = (double)format.getSampleRate() / windowSize;
				double amp = 0;
				double lowAmp = 0;
				double highAmp = 0;
				for (int j = 0; j < windowSize/2; j++) {
					double freq = (j+1) * binHz;
					double norm = Math.sqrt(spectrumActual[j]*spectrumActual[j] + spectrumImaginal[j]*spectrumImaginal[j]);
					//System.out.println(i+","+j+","+norm);
					amp += norm;
					if (freq < lowTh) {
						lowAmp += norm;
					}
					if (freq > HighTh) {
						highAmp += norm;
					}
				}
				
			}
		}
	}
	
	
	/**
     * 離散フーリエ変換 http://krr.blog.shinobi.jp/javafx_praxis/java%E3%81%A7%E5%91%A8%E6%B3%A2%E6%95%B0%E5%88%86%E6%9E%90%E3%82%92%E3%81%97%E3%81%A6%E3%81%BF%E3%82%8B
     * @param in フーリエ変換を行う実数配列
     * @param outActual 計算結果の実数部配列
     * @param outImaginal 計算結果の虚数部配列
     * @param winFlg 窓関数の使用フラグ
     */
    void dft( double[] in , double[] outActual , double[] outImaginal , boolean winFlg )
    {
        // 配列初期化
        int  length             = in.length;
         
        // 離散フーリエ変換
        for( int k=0 ; k<length ; k++ )
        {
            // 初期化
            outActual[k]    = 0.0d;
            outImaginal[k]  = 0.0d;
             
            // 計算
            for( int n=0 ; n<length ; n++ )
            {
                // 入力値に窓関数を適用
                double normal   = ( !winFlg )? in[n]  : hanWindow( in[n] , n , 0 , length );
                 
                // k次高周波成分を計算
                outActual[k]    +=        normal * Math.cos( 2.0 * Math.PI * (double)n * (double)k / (double)length );
                outImaginal[k]  += -1.0 * normal * Math.sin( 2.0 * Math.PI * (double)n * (double)k / (double)length );
            }
             
            // 残りの計算
            //outActual[k]    /= length;
            //outImaginal[k]  /= length;
        }
    }
    
    /**
     * 窓関数（ハン窓）
     * @param in 変換する値
     * @param i 配列中のインデックス
     * @param minIndex 配列の最小インデックス
     * @param maxIndex 配列の最大インデックス
     * @return
     */
    protected double hanWindow( double in , double i , double minIndex , double maxIndex )
    {
        // 入力値の正規化
        double normal   = i / ( maxIndex - minIndex );
         
        // ハン窓関数の値を取得
        double  han     =  0.5 - 0.5 * Math.cos( 2.0 * Math.PI * normal );
         
        return in * han;
    }
    
   
    /**
     * 音声ファイルを読み込み、メタ情報とサンプリング・データを取得
     * @throws Exception
     */
    protected void initialize() throws Exception
    {
        // 音声ストリームを取得
        File                file    = new File( fileName );
        AudioInputStream    is      = AudioSystem.getAudioInputStream( file );
         
        // メタ情報の取得
        System.out.println( format.toString() );
         
        // 取得する標本数を計算
        // 1秒間で取得した標本数がサンプルレートであることから計算
        int mount = (int)is.getFrameLength();
         
        // 音声データの取得
        valuesActual    = new double[ mount ];
        valuesImaginal  = new double[ mount ];
        for( int i=0 ; i<mount ; i++ )
        {
            // 1標本分の値を取得
            int     size        = format.getFrameSize();
            byte[]  data        = new byte[ size ];
            int     readedSize  = is.read(data);
             
            // データ終了でループを抜ける
            if( readedSize == -1 ){ break; } 
             
            // 1標本分の値を取得
            switch( format.getSampleSizeInBits() )
            {
                case 8:
                    valuesActual[i]   = (int) data[0];
                    break;
                case 16:
                    valuesActual[i]   = (int) ByteBuffer.wrap( data ).order( ByteOrder.LITTLE_ENDIAN ).getShort();
                    break;
                default:
            }
        }
         
        // 音声ストリームを閉じる
        is.close();
    }
}

