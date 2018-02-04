import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import java.io.*;

public class WavPlayer {
	
	AudioInputStream ais;
	
	public Clip loadWav(String fname) {
		Clip clip = null;	//try���G���[�ŏI�������Ƃ��̏���
		try {
			AudioInputStream aistream = AudioSystem.getAudioInputStream(new File(fname));
			DataLine.Info info = new DataLine.Info(Clip.class ,aistream.getFormat());
			clip = (Clip)AudioSystem.getLine(info);
			clip.open(aistream);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		return clip;
	}

	//�G���g���[�|�C���g
	public static void main(String[] args) {
		//�E�C���h�E�쐬
		JFrame frame = new JFrame("WAV Player");
		frame.setBounds( 0, 0, 100, 100);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//�C���X�^���X�쐬
		WavPlayer wavp = new WavPlayer();
		//�t�@�C���ǂݍ���
		Clip clip = wavp.loadWav("A08_002.wav");
		//�Đ�
		clip.start();
	}
}