import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import java.io.*;

public class WavPlayer {
	public Clip loadWav(String fname) {
		Clip clip = null;	//tryがエラーで終了したときの準備
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

	//エントリーポイント
	public static void main(String[] args) {
		//ウインドウ作成
		JFrame frame = new JFrame("WAV Player");
		frame.setBounds( 0, 0, 100, 100);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//インスタンス作成
		WavPlayer wavp = new WavPlayer();
		//ファイル読み込み
		Clip clip = wavp.loadWav("A08_002.wav");
		//再生
		clip.start();
	}
}