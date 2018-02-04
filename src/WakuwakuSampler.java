import javax.sound.sampled.*;
import java.io.*;
import java.nio.*;
import java.util.Arrays;
import java.util.*;
import javax.sound.midi.*;
import javax.swing.*;
import javax.imageio.*;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Toolkit;

public class WakuwakuSampler implements Runnable {
	AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
	File wavFile;
	
	String rhythmMidiName = "mhd2018_arab_midi.mid";
	
	static final float SAMPLE_RATE = 22050;
	static final int SAMPLE_SIZE_IN_BITS = 16;
	static final int CHANNELS = 1;
	static final boolean SIGNED = true;
	static final boolean BIG_ENDIAN = true;
	
	static int BPM = 100;
	
	static int lowTh = 200;
	static int HighTh = 2000;
	static int WindowSize = 4096;
	
	TargetDataLine line;
	String fileName;
	AudioFormat format;
	double maxAmp = 0;
	double normalizeRatio;
	
	Track rhythmTrack;
	
	VideoCapture cap;
	WavPlayer wavp;
	
	int bdI = -1;
	int sdI = -1;
	int hhI = -1;
	int voiceStart, voiceEnd;
	
	List<Double> amps, lowAmps, middleAmps, highAmps;

	double[] valuesActual;

	public static void main(String[] args) throws Exception {
		new WakuwakuSampler();
	}
	
	public WakuwakuSampler() throws Exception {
		rhythmTrack = readMidi(rhythmMidiName);
		/*
		Sequence sequence = new Sequence(Sequence.PPQ, 480);
		Track track = sequence.createTrack();
		for (int i = 0; i < rhythmTrack.size(); i++) {
			MidiEvent event = rhythmTrack.get(i);
			track.add(event);
		}
		Synthesizer synthesizer = MidiSystem.getSynthesizer();
		Instrument[] insts = synthesizer.getAvailableInstruments();
		for (Instrument inst: insts) {
			if (inst.toString().startsWith("Drumkit")) {
				System.out.println(inst);
				System.out.println(" "+inst.);
			}
		}
		*/
		//synthesizer.remapInstrument(, );
	    //synthesizer.open();
	    
		
		format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        line = AudioSystem.getTargetDataLine(format);
        line.open(format);
        cap = new VideoCapture();

        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        System.err.println("Press Enter to START Recording...");
        Thread capThread = new Thread(cap);
        capThread.start();
        System.err.println("Waiting the Open of Web Camera...");
        while (! cap.isOpen()) {}
        
        in.readLine();
        System.err.println("Record Started!");
        Thread thread = new Thread(this);
        thread.start();

        
         
        System.err.println("Press Enter to STOP Recording...");
        in.readLine();
        stopRecording();
        cap.pause();
        
        initialize();
        dft(WindowSize);
        
		//int max = (int)Math.pow(2, SAMPLE_SIZE_IN_BITS) * WindowSize;
		//System.out.println("ratio: "+ (maxAmp / max));
		
        extractBassDrum();
        extractSnareDrum();
        extractHighHat();
        extractVoice();
        
        
        
        System.out.println(iToMilli(bdI));
        WakuwakuPanel panel = new WakuwakuPanel();
        JFrame frame = new JFrame("");
	    frame.setBounds(100, 100, 700, 700);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.add(panel);
	    frame.setVisible(true);
        String bdImg = cap.getNearFile(iToMilli(bdI));
        String sdImg = cap.getNearFile(iToMilli(sdI));
        String hhImg = cap.getNearFile(iToMilli(hhI));
        
		//Sequencer sequencer = MidiSystem.getSequencer(false);
		//sequencer.open();
        
        wavp = new WavPlayer();
        Clip bdClip = wavp.loadWav("bd.wav");
        Clip hhClip = wavp.loadWav("hh.wav");
        Clip sdClip = wavp.loadWav("sd.wav");
        Clip voiceClip = wavp.loadWav("voice.wav");
        
        Cycle c2 = new Cycle(2);
        c2.setFlag(1, true);
        
        Cycle c3 = new Cycle(3);
        c3.setFlag(1, true);
        
        Cycle c4 = new Cycle(4);
        c4.setFlag(0, true);
        
        Cycle c6 = new Cycle(6);
        c4.setFlag(1, true);
        c4.setFlag(2, true);
        c4.setFlag(5, true);
        
        Cycle c8 = new Cycle(8);
        c8.setFlag(0, true);
        
        for (int i = 0; i < 512; i++) {
        	
        	if (c2.next()) {
        		play("hh.wav");
        		panel.setImgFile(hhImg);
        	}
        	if (c3.next()) {
        		play("sd.wav");
        		panel.setImgFile(sdImg);
        	}
        	if (c4.next()) {
        		play("bd.wav");
        		panel.setImgFile(bdImg);
        	}
        	if (c6.next()) {
        		play("sd.wav");
        	}
        	if (c8.next()) {
        		play("voice.wav");
        	}
        	Thread.currentThread().sleep(300);
        }
	}
	
	void play(String wav) {
		Clip clip = wavp.loadWav(wav);
		clip.start();
	}
	
	int voiceLen() {
		int beats = 4;
		return (int)Math.round((beats*60/BPM) * format.getSampleRate());
	}
	
	void extractVoice() {
		int len = (int)Math.floor(voiceLen() / WindowSize);
		voiceStart = 0;
		voiceEnd = len;
		double max = 0;
		for (int i = 0; i < amps.size() - len - 1; i++) {
			double sum = 0;
			for (int j = i; j <= i + len; j++) {
				sum += amps.get(j);
			}
			if (sum > max && ! containsDrums(i, i+len)) {
				max = sum;
				voiceStart = i;
				voiceEnd = i + len;

			}
		}
		byte[] bytes = extractSound(voiceStart, voiceEnd);
		saveSound(bytes, "voice.wav");
	}
	
	boolean containsDrums(int i, int j) {
		if (i <= bdI && bdI <= j) { return true; }
		if (i <= sdI && sdI <= j) { return true; }
		if (i <= hhI && hhI <= j) { return true; }
		return false;
	}
	
	int iToMilli(int i) {
		int frame = i * WindowSize;
		double t = frame / format.getFrameRate();
		return (int)Math.floor(t * 1000);
	}
	
	int startFrame(int i) {
		return i * WindowSize;
	}

	int endFrame(int i) {
		return (i+1)*WindowSize - 1;
	}
	
	void valueToByte(byte[] buffer, int i, double v) {
		if (format.getSampleSizeInBits() == 8) {
			buffer[i] = (byte)v;
		} else if (format.getSampleSizeInBits() == 16) {
			short sample = (short)v;
			if (format.isBigEndian()) {
				buffer[i] = (byte) (sample>>>8);
				buffer[i+1] = (byte) (sample & 0xff);
			} else {
				// FIXME
			}
		}
	}
	
	byte[] extractSound(int i, int j) {
		int sampSizeInBytes = (int)(format.getSampleSizeInBits() / 8);
		int start = startFrame(i);
		int end = endFrame(j);
		double[] buffer = Arrays.copyOfRange(valuesActual, start, end+1);//new double[end - start + 1];
		normalize(buffer);
		byte[] bytes = new byte[buffer.length * sampSizeInBytes];
		for (int k = 0; k < buffer.length; k++) {
			int v = (int)buffer[k];
			//System.err.println(v);
			valueToByte(bytes, k * sampSizeInBytes, v);
		}
		return bytes;
	}
	
	
	void extractBassDrum() {
		double max = 0;
		for (int i = 0; i < lowAmps.size(); i++) {
			if (max < lowAmps.get(i)) {
				max = lowAmps.get(i);
				bdI = i;
			}
		}
		byte[] bytes = extractSound(bdI, bdI);
		saveSound(bytes, "bd.wav");
		
	}
	
	void extractSnareDrum() {
		double max = 0;
		for (int i = 0; i < middleAmps.size(); i++) {
			if (max < middleAmps.get(i)) {
				max = middleAmps.get(i);
				sdI = i;
			}
		}
		byte[] bytes = extractSound(sdI, sdI);
		saveSound(bytes, "sd.wav");
	}

	
	void extractHighHat() {
		double max = 0;
		for (int i = 0; i < highAmps.size(); i++) {
			if (max < highAmps.get(i)) {
				max = highAmps.get(i);
				hhI = i;
			}
		}
		byte[] bytes = extractSound(hhI, hhI);
		saveSound(bytes, "hh.wav");
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
		amps = new ArrayList<Double>();
		lowAmps = new ArrayList<Double>();
		middleAmps = new ArrayList<Double>();
		highAmps = new ArrayList<Double>();
		for (int i = 0; i < valuesActual.length; i += windowSize) {
			double t = iToTime(i);
			System.out.println("t = "+t+" [sec]");
			if (valuesActual.length - i >= windowSize) { 
				double[] spectrumActual = new double[ windowSize ];
				double[] spectrumImaginal = new double[ windowSize ];
				//System.out.println("### i: "+i+", array size: "+valuesActual.length);
				double[] valuesInWindow = Arrays.copyOfRange(valuesActual, i, i+windowSize);
				
				dft( valuesInWindow , spectrumActual , spectrumImaginal , false );
				double binHz = (double)format.getSampleRate() / windowSize;
				double amp = 0;
				double lowAmp = 0;
				double middleAmp = 0;
				double highAmp = 0;
				for (int j = 0; j < windowSize/2; j++) {
					double freq = (j+1) * binHz;
					double norm = Math.sqrt(spectrumActual[j]*spectrumActual[j] + spectrumImaginal[j]*spectrumImaginal[j]);
					//System.out.println(i+","+j+","+norm);
					amp += norm;
					if (freq < lowTh) {
						lowAmp += norm;
					}
					if (lowTh < freq && freq < HighTh ) {
						middleAmp += norm;
					}
					if (freq > HighTh) {
						highAmp += norm;
					}
				}
				amps.add(amp);
				lowAmps.add(lowAmp);
				middleAmps.add(middleAmp);
				highAmps.add(highAmp);
				if (amp > maxAmp) { maxAmp = amp; }
				
			}
		}
	}
	
	double iToTime(int i) {
		return i / format.getSampleRate();
	}
	
	Track readMidi(String midiFileName) throws Exception {
	    Track[] tracks = MidiSystem.getSequence(new File(midiFileName)).getTracks();
	    //System.out.println("track count: "+ tracks.length);
	    return tracks[0];
	}
	
    void dft( double[] in , double[] outActual , double[] outImaginal , boolean winFlg )
    {
        int  length             = in.length;
         
        for( int k=0 ; k<length ; k++ )
        {
            outActual[k]    = 0.0d;
            outImaginal[k]  = 0.0d;
             
            for( int n=0 ; n<length ; n++ )
            {
                double normal   = ( !winFlg )? in[n]  : hanWindow( in[n] , n , 0 , length );
                 
                outActual[k]    +=        normal * Math.cos( 2.0 * Math.PI * (double)n * (double)k / (double)length );
                outImaginal[k]  += -1.0 * normal * Math.sin( 2.0 * Math.PI * (double)n * (double)k / (double)length );
            }
             
            //outActual[k]    /= length;
            //outImaginal[k]  /= length;
        }
    }
    
    protected double hanWindow( double in , double i , double minIndex , double maxIndex )
    {
        double normal   = i / ( maxIndex - minIndex );
        double  han     =  0.5 - 0.5 * Math.cos( 2.0 * Math.PI * normal );
        return in * han;
    }
    
   
    /**
     *
     * @throws Exception
     */
    protected void initialize() throws Exception
    {
        File                file    = new File( fileName );
        AudioInputStream    is      = AudioSystem.getAudioInputStream( file );
         
        System.out.println( format.toString() );
         
        int mount = (int)is.getFrameLength();
        if (mount < 0) {
        	System.err.println("Sorry, recording failed...");
        	System.exit(-1);
        }
        valuesActual    = new double[ mount ];
        //valuesImaginal  = new double[ mount ];
        double max = 0;
        for ( int i=0 ; i<mount ; i++ ) {
            int     size        = format.getFrameSize();
            byte[]  data        = new byte[ size ];
            int     readedSize  = is.read(data);
             
            if( readedSize == -1 ){ break; } 
             
            switch( format.getSampleSizeInBits() )
            {
                case 8:
                    valuesActual[i]   = (int) data[0];
                    break;
                case 16:
                	valuesActual[i]   = (int) ByteBuffer.wrap( data ).order( ByteOrder.LITTLE_ENDIAN ).getShort();
                	/*
                	if (BIG_ENDIAN) {
                		valuesActual[i]   = (int) ByteBuffer.wrap( data ).order( ByteOrder.BIG_ENDIAN ).getShort();
                	} else {
                		valuesActual[i]   = (int) ByteBuffer.wrap( data ).order( ByteOrder.LITTLE_ENDIAN ).getShort();
                	}
                	*/
                    break;
                default:
            }
            if(Math.abs(valuesActual[i]) > max) {
            	max = Math.abs(valuesActual[i]);
            }
        }
         
        is.close();
        
        normalize(valuesActual, max);
        
    }
    
    void normalize(double[] buf) {
    	double max = 0;
    	for (double v : buf) {
    		if (v > max) { max = v; }
    	}
    	normalize(buf, max);
    }
    
    void normalize(double[] buf, double max) {
    	double targetMax = Math.pow(2, format.getSampleSizeInBits()-1) * 0.9;
    	double normalizeRatio = targetMax / max;
    	//System.err.println("normalizeRatio: "+normalizeRatio);
    	for ( int i=0 ; i < buf.length ; i++ ) {
        	buf[i] *= normalizeRatio;
        }
    	
    }
    
    void saveSound(byte[] bytes, String fileName) {
    	AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(bytes), format, bytes.length);
    	try {
    		AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(fileName));
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
}


class WakuwakuPanel extends JPanel {
	
	String fileName = null;
	

	
	public void setImgFile(String fileName) {
		this.fileName = fileName;
	}
	
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (fileName != null) {
			Image img = Toolkit.getDefaultToolkit().getImage(fileName);

			g.drawImage(img, 10, 10, this);
		}
    }
	
	
}

class Cycle {
	int base;
	int i;
	HashMap<Integer, Boolean> flags;
	
	Cycle(int base) {
		this.base = base;
		i = 0;
		flags = new HashMap<Integer,Boolean>();
		for (int k = 0; k < base; k++) {
			flags.put(k, false);
		}
	}
	
	void setFlag(int k, boolean flag) {
		flags.put(k, flag);
	}
	
	boolean next() {
		boolean ret = flags.get(i);
		i++;
		if (i >= base) { i = 0; }
		return ret;
	}
}

