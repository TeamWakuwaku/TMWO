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
	boolean webcamFlag = false;
	Webcam webcam = Webcam.getDefault();

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
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open();
		webcamFlag = true;
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
				ImageIO.write(webcam.getImage(), "png", new File("images/"+s+".png"));
				try {
					Thread.sleep(50);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
	
	public boolean isOpen() {
		return webcam.isOpen();
	}
	
	public String getNearFile(int milli) {
		File dir = new File("images");
		File[] files = dir.listFiles();
		int diff=Integer.MAX_VALUE;
		String ans="";
		//if file doesn't exist or empty
		if (files == null || files.length == 0) {
			return "error occured";
		}
		
		for(int i=0; i<files.length; i++) {
			String fileName = files[i].getName();
			String fileMilliStr = fileName.substring(0, fileName.length()-4);
			int fileMilli = Integer.parseInt(fileMilliStr);
			if(diff > Math.abs(fileMilli - milli)) {
				diff = Math.abs(fileMilli - milli);
				ans = "images/"+files[i].getName();
			}
		}
		return ans;
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
