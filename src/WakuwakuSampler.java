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
     * ���U�t�[���G�ϊ� http://krr.blog.shinobi.jp/javafx_praxis/java%E3%81%A7%E5%91%A8%E6%B3%A2%E6%95%B0%E5%88%86%E6%9E%90%E3%82%92%E3%81%97%E3%81%A6%E3%81%BF%E3%82%8B
     * @param in �t�[���G�ϊ����s�������z��
     * @param outActual �v�Z���ʂ̎������z��
     * @param outImaginal �v�Z���ʂ̋������z��
     * @param winFlg ���֐��̎g�p�t���O
     */
    void dft( double[] in , double[] outActual , double[] outImaginal , boolean winFlg )
    {
        // �z�񏉊���
        int  length             = in.length;
         
        // ���U�t�[���G�ϊ�
        for( int k=0 ; k<length ; k++ )
        {
            // ������
            outActual[k]    = 0.0d;
            outImaginal[k]  = 0.0d;
             
            // �v�Z
            for( int n=0 ; n<length ; n++ )
            {
                // ���͒l�ɑ��֐���K�p
                double normal   = ( !winFlg )? in[n]  : hanWindow( in[n] , n , 0 , length );
                 
                // k�������g�������v�Z
                outActual[k]    +=        normal * Math.cos( 2.0 * Math.PI * (double)n * (double)k / (double)length );
                outImaginal[k]  += -1.0 * normal * Math.sin( 2.0 * Math.PI * (double)n * (double)k / (double)length );
            }
             
            // �c��̌v�Z
            //outActual[k]    /= length;
            //outImaginal[k]  /= length;
        }
    }
    
    /**
     * ���֐��i�n�����j
     * @param in �ϊ�����l
     * @param i �z�񒆂̃C���f�b�N�X
     * @param minIndex �z��̍ŏ��C���f�b�N�X
     * @param maxIndex �z��̍ő�C���f�b�N�X
     * @return
     */
    protected double hanWindow( double in , double i , double minIndex , double maxIndex )
    {
        // ���͒l�̐��K��
        double normal   = i / ( maxIndex - minIndex );
         
        // �n�����֐��̒l���擾
        double  han     =  0.5 - 0.5 * Math.cos( 2.0 * Math.PI * normal );
         
        return in * han;
    }
    
   
    /**
     * �����t�@�C����ǂݍ��݁A���^���ƃT���v�����O�E�f�[�^���擾
     * @throws Exception
     */
    protected void initialize() throws Exception
    {
        // �����X�g���[�����擾
        File                file    = new File( fileName );
        AudioInputStream    is      = AudioSystem.getAudioInputStream( file );
         
        // ���^���̎擾
        System.out.println( format.toString() );
         
        // �擾����W�{�����v�Z
        // 1�b�ԂŎ擾�����W�{�����T���v�����[�g�ł��邱�Ƃ���v�Z
        int mount = (int)is.getFrameLength();
         
        // �����f�[�^�̎擾
        valuesActual    = new double[ mount ];
        valuesImaginal  = new double[ mount ];
        for( int i=0 ; i<mount ; i++ )
        {
            // 1�W�{���̒l���擾
            int     size        = format.getFrameSize();
            byte[]  data        = new byte[ size ];
            int     readedSize  = is.read(data);
             
            // �f�[�^�I���Ń��[�v�𔲂���
            if( readedSize == -1 ){ break; } 
             
            // 1�W�{���̒l���擾
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
         
        // �����X�g���[�������
        is.close();
    }
}

