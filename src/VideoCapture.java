package info.video;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;


public class VideoCapture implements Runnable {
	boolean stopped = false;

	public static void main(String args[]) throws IOException{
		//doJob();
	}

/*	public static void doJob() {
		int ms;
		long startTime, endTime, diffTime;
		Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open();
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		cal.setTime(new Date());

		File dir = new File("images");
		//imagesフォルダがあれば削除
		delete(dir);
		dir.mkdir();
		
		startTime = System.currentTimeMillis();
		while(!stopped) {
			try {
                endTime = System.currentTimeMillis();
        			diffTime = (endTime - startTime);
				String s = String.valueOf(diffTime);
				ImageIO.write(webcam.getImage(), "png", new File("images/cap"+s+".png"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		//webcam.close();
	}*/
	
	@Override
	public void run() {
		int ms;
		long startTime, endTime, diffTime;
		Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open();
		Calendar cal = Calendar.getInstance(TimeZone.getDefault());
		cal.setTime(new Date());

		File dir = new File("images");
		//imagesフォルダがあれば削除
		delete(dir);
		dir.mkdir();
		
		startTime = System.currentTimeMillis();
		while(!stopped) {
			try {
                endTime = System.currentTimeMillis();
        			diffTime = (endTime - startTime);
				String s = String.valueOf(diffTime);
				ImageIO.write(webcam.getImage(), "png", new File("images/cap"+s+".png"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		//webcam.close();
	}
	
	public void pause() {
		stopped = true;
	}
	
	 /*
     * ファイルおよびディレクトリを削除する
     */
    private static void delete(File f)
    {
        /*
         * ファイルまたはディレクトリが存在しない場合は何もしない
         */
        if(f.exists() == false) {
            return;
        }

        if(f.isFile()) {
            /*
             * ファイルの場合は削除する
             */
            f.delete();

        } else if(f.isDirectory()){
            /*
             * ディレクトリの場合は、すべてのファイルを削除する
             */

            /*
             * 対象ディレクトリ内のファイルおよびディレクトリの一覧を取得
             */
            File[] files = f.listFiles();

            /*
             * ファイルおよびディレクトリをすべて削除
             */
            for(int i=0; i<files.length; i++) {
                /*
                 * 自身をコールし、再帰的に削除する
                 */
                delete( files[i] );
            }
            /*
             * 自ディレクトリを削除する
             */
            f.delete();
        }
    }
}