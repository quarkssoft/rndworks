package com.imagecomp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class BService extends Service {
	private WakeLock mWakeLock = null;
	String TAG = "HelloWorld";
	public static final int SCREEN_OFF_RECEIVER_DELAY = 60*200;
	Context cnt = null;
	//sharedpreference
	public static String MyPREFERENCES  ;
	   public static  String COUNT;
	   SharedPreferences sharedpreferences;
	   int countCursor =0;
	   static Uri bufferUri;
	   //////////////////////////////////
	
		private static Bitmap bmp, yourSelectedImage, bmpimg1, bmpimg2;
		
		private static String path1, path2;
		private static String text;
		
		private static Uri selectedImage;
		private static InputStream imageStream;
		private static long startTime, endTime;
		private static final int SELECT_PHOTO = 100;

		private static int descriptor = DescriptorExtractor.BRISK;
		private static String descriptorType;
		private static int min_dist = 10;
		private static int min_matches = 750;
		///////////////////////////////////////////////////
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		cnt= this.getApplication();
		MyPREFERENCES= getResources().getString(R.string.shared_preference_name);
		COUNT = getResources().getString(R.string.shr_count_details);
		 sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
	}
	
	private void registerListener(){
		
		final Handler handler = new Handler();
	    Timer timer = new Timer();

	    TimerTask task = new TimerTask() {       
	        @Override
	        public void run() {
	            handler.post(new Runnable() {
	                public void run() {       
	                    try {
	                      // Toast.makeText(cnt, "Running success", Toast.LENGTH_SHORT).show();
	                       getImageLoc();
	                    } catch (Exception e) {
	                        // error, do something
	                    	
	                    	/*Toast.makeText(cnt, e.toString(), Toast.LENGTH_LONG).show();*/
	                    	Log.e(TAG, e.toString());
	                    }
	                }
	            });
	        }
	    };

	    timer.schedule(task, 0, SCREEN_OFF_RECEIVER_DELAY);  // interval of one minute
		
		
		
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		startForeground(Process.myPid(), new Notification());
		registerListener();
		mWakeLock.acquire();

		return START_STICKY;
	}
	
	//Get imageLocation
	
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void getImageLoc(){
		
		try {
			int sharedP = sharedpreferences.getInt(COUNT, 0);
			System.out.println("Shared P " +sharedP);
			Uri newUri = handleImageUri(getUri());
			String[] projection = { MediaStore.Images.Media._ID,MediaStore.Images.Media.DATA };
			Cursor cursor = getContentResolver().query(newUri, projection,null, null, MediaStore.Images.Media._ID);
			 if(cursor != null){
			int count = cursor.getCount();
			System.out.println("Count :: " +count);
			SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putInt(COUNT, count);
            editor.commit();
            
			if(sharedP > 0 && count != sharedP){
				System.out.println("Cursor is greater than SharedP :::: ");
			int image_column_index = cursor.getColumnIndex(MediaStore.Images.Media._ID);
			int image_path_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
			StringBuffer buffLocat = new StringBuffer();
			for (int i = count; i >sharedP; i--) {
				
				cursor.moveToPosition(i-1);
				
				System.out.println("URL :: "+cursor.getString(image_path_index));
				buffLocat.append(cursor.getString(image_path_index));
				//Toast.makeText(getApplicationContext(), "Hello :: "+cursor.getString(image_path_index), Toast.LENGTH_LONG).show();
				File f = new File(cursor.getString(image_path_index));  //  
	             bufferUri = Uri.fromFile(f);
	             System.out.println(bufferUri);
			}
			
			if(buffLocat.length()>0){
				//Toast.makeText(getApplicationContext(), "Added New photo", Toast.LENGTH_LONG).show();
				
				// Compare Starts//////////////////////////////////////////////////////////// OpenCv ///////////////////////////////////////////////////
				Intent intent = new Intent("org.opencv.engine.BIND");
			    intent.setPackage("org.opencv.engine");
				bindService(intent, mServerConn, Context.BIND_AUTO_CREATE);
				  
				if (!OpenCVLoader.initDebug()) {
		            Log.d("ERROR", "Unable to load OpenCV");
		        } else {
		            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		        }
				run();
				runAtFirst();
			}
			}else{
				//Toast.makeText(getApplicationContext(), "No Images download yet", Toast.LENGTH_SHORT).show();
			}
			
			 }
		} catch (Exception e) {

			//Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
			System.out.println(e.toString());
		}
	}
	
	//Workaround for getting image location after Lollipop 
	
	public static Uri handleImageUri(Uri uri) {
	    Pattern pattern = Pattern.compile("(content://media/.*\\d)");
	    if (uri.getPath().contains("content")) {
	        Matcher matcher = pattern.matcher(uri.getPath());
	        if (matcher.find())
	            return Uri.parse(matcher.group(1));
	        else
	            throw new IllegalArgumentException("Cannot handle this URI");
	    } else
	        return uri;
	}
	
	private Uri getUri() {
	    String state = Environment.getExternalStorageState();
	    if(!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
	        return MediaStore.Images.Media.INTERNAL_CONTENT_URI;
	    }

	    return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	}
	/////////////////////////////Image Compare OpenCV /////////////////////////////////////////////////////
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
	
	protected ServiceConnection mServerConn = new ServiceConnection() {
	   
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			// TODO Auto-generated method stub
			System.out.println("Workaround successs");
			Log.i(TAG, "Workaround success");
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
		System.out.println("Workaround fail");	
		Log.i(TAG, "Workaround fails");
		}
	};
	
	public void run() {
		if (descriptor == DescriptorExtractor.BRIEF)
			descriptorType = "BRIEF";
		else if (descriptor == DescriptorExtractor.BRISK)
			descriptorType = "BRISK";
		else if (descriptor == DescriptorExtractor.FREAK)
			descriptorType = "FREAK";
		else if (descriptor == DescriptorExtractor.ORB)
			descriptorType = "ORB";
		else if (descriptor == DescriptorExtractor.SIFT)
			descriptorType = "SIFT";
		else if(descriptor == DescriptorExtractor.SURF)
			descriptorType = "SURF";
		System.out.println(descriptorType);
		
		
		
	}
	
	
	private void runAtFirst(){

		// TODO Auto-generated method stub
		imageSelection(ResourceToUri(getApplicationContext(), R.drawable.leena),1);
		imageSelection(bufferUri,2);
		if (bmpimg1 != null && bmpimg2 != null) {
			
			bmpimg1 = Bitmap.createScaledBitmap(bmpimg1, 100, 100, true);
			bmpimg2 = Bitmap.createScaledBitmap(bmpimg2, 100, 100, true);
			Mat img1 = new Mat();
			Utils.bitmapToMat(bmpimg1, img1);
	        Mat img2 = new Mat();
	        Utils.bitmapToMat(bmpimg2, img2);
	        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGBA2GRAY); 
	        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGBA2GRAY); 
	        img1.convertTo(img1, CvType.CV_32F);
	        img2.convertTo(img2, CvType.CV_32F);
	        
	        //Log.d("ImageComparator", "img1:"+img1.rows()+"x"+img1.cols()+" img2:"+img2.rows()+"x"+img2.cols());
	        Mat hist1 = new Mat();
	        Mat hist2 = new Mat();
	        MatOfInt histSize = new MatOfInt(180);
	        MatOfInt channels = new MatOfInt(0);
	        ArrayList<Mat> bgr_planes1= new ArrayList<Mat>();
	        ArrayList<Mat> bgr_planes2= new ArrayList<Mat>();
	        Core.split(img1, bgr_planes1);
	        Core.split(img2, bgr_planes2);
	        MatOfFloat histRanges = new MatOfFloat (0f, 180f);		        
	        boolean accumulate = false;
	        Imgproc.calcHist(bgr_planes1, channels, new Mat(), hist1, histSize, histRanges, accumulate);
        	Core.normalize(hist1, hist1, 0, hist1.rows(), Core.NORM_MINMAX, -1, new Mat());
	        Imgproc.calcHist(bgr_planes2, channels, new Mat(), hist2, histSize, histRanges, accumulate);
        	Core.normalize(hist2, hist2, 0, hist2.rows(), Core.NORM_MINMAX, -1, new Mat());
		        img1.convertTo(img1, CvType.CV_32F);
		        img2.convertTo(img2, CvType.CV_32F);
		        hist1.convertTo(hist1, CvType.CV_32F);
		        hist2.convertTo(hist2, CvType.CV_32F);
		
	        	double compare = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CHISQR);
	        	Log.d("ImageComparator", "compare: "+compare);
	        	if(compare>0 && compare<1500) {
	        		//Toast.makeText(MainActivity.this, "Images may be possible duplicates, verifying", Toast.LENGTH_LONG).show();
	        		new asyncTask(this).execute();
	        	}
	        	else if(compare==0){
	        		
	        		deleteImage(bufferUri.getPath());
	        		
	        		
	        	}
	        	
			startTime = System.currentTimeMillis();
		} 
	}


	public static Uri ResourceToUri (Context context,int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID) );
    }

	

	
	//Ram Written
	
	private void imageSelection(Uri imagePath,int imgNo){
		//select ImageURI
		//selectedImage = imageReturnedIntent.getData();
		selectedImage =imagePath;
		try {
			imageStream = getContentResolver().openInputStream(
					selectedImage);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		yourSelectedImage = BitmapFactory.decodeStream(imageStream);
		if (imgNo == 1) {
			//iv1.setImageBitmap(yourSelectedImage);
			path1 = selectedImage.getPath();
			bmpimg1 = yourSelectedImage;
			//iv1.invalidate();
		} else if (imgNo == 2) {
			//iv2.setImageBitmap(yourSelectedImage);
			path2 = selectedImage.getPath();
			bmpimg2 = yourSelectedImage;
			//iv2.invalidate();
		}
	}
	
	
	public static class asyncTask extends AsyncTask<Void, Void, Void> {
		private static Mat img1, img2, descriptors, dupDescriptors;
		private static FeatureDetector detector;
		private static DescriptorExtractor DescExtractor;
		private static DescriptorMatcher matcher;
		private static MatOfKeyPoint keypoints, dupKeypoints;
		private static MatOfDMatch matches, matches_final_mat;
		//private static ProgressDialog pd;
		private static boolean isDuplicate = false;
		private BService asyncTaskContext=null;
		private static Scalar RED = new Scalar(255,0,0);
		private static Scalar GREEN = new Scalar(0,255,0);
		public asyncTask(BService bService)
		{
			asyncTaskContext=bService;
		}
		@Override
		protected void onPreExecute() {
		
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
			compare();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			try {
				Mat img3 = new Mat();
				MatOfByte drawnMatches = new MatOfByte();
				Features2d.drawMatches(img1, keypoints, img2, dupKeypoints,
						matches_final_mat, img3, GREEN, RED,  drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
				bmp = Bitmap.createBitmap(img3.cols(), img3.rows(),
						Bitmap.Config.ARGB_8888);
				Imgproc.cvtColor(img3, img3, Imgproc.COLOR_BGR2RGB);
				Utils.matToBitmap(img3, bmp);
				List<DMatch> finalMatchesList = matches_final_mat.toList();
				final int matchesFound=finalMatchesList.size();
				endTime = System.currentTimeMillis();
				if (finalMatchesList.size() > min_matches)// dev discretion for
														// number of matches to
														// be found for an image
														// to be judged as
														// duplicate
				{
					text = finalMatchesList.size()
							
							+ " matches were found. Possible duplicate image.\nTime taken="
							+ (endTime - startTime) + "ms";
					isDuplicate = true;
					
					asyncTaskContext.deleteImage(bufferUri.getPath());
				
				} else {
					text = finalMatchesList.size()
							+ " matches were found. Images aren't similar.\nTime taken="
							+ (endTime - startTime) + "ms";
					isDuplicate = false;
				
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(e.toString());
			}
		}
		
		

		
		void compare() {
			try {
				bmpimg1 = bmpimg1.copy(Bitmap.Config.ARGB_8888, true);
				bmpimg2 = bmpimg2.copy(Bitmap.Config.ARGB_8888, true);
				img1 = new Mat();
				img2 = new Mat();
				Utils.bitmapToMat(bmpimg1, img1);
				Utils.bitmapToMat(bmpimg2, img2);
				Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2RGB);
				Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGR2RGB);
				detector = FeatureDetector.create(FeatureDetector.PYRAMID_FAST);
				DescExtractor = DescriptorExtractor.create(descriptor);
				matcher = DescriptorMatcher
						.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

				keypoints = new MatOfKeyPoint();
				dupKeypoints = new MatOfKeyPoint();
				descriptors = new Mat();
				dupDescriptors = new Mat();
				matches = new MatOfDMatch();
				detector.detect(img1, keypoints);
				Log.d("LOG!", "number of query Keypoints= " + keypoints.size());
				detector.detect(img2, dupKeypoints);
				Log.d("LOG!", "number of dup Keypoints= " + dupKeypoints.size());
				// Descript keypoints
				DescExtractor.compute(img1, keypoints, descriptors);
				DescExtractor.compute(img2, dupKeypoints, dupDescriptors);
				Log.d("LOG!", "number of descriptors= " + descriptors.size());
				Log.d("LOG!",
						"number of dupDescriptors= " + dupDescriptors.size());
				// matching descriptors
				matcher.match(descriptors, dupDescriptors, matches);
				Log.d("LOG!", "Matches Size " + matches.size());
				// New method of finding best matches
				List<DMatch> matchesList = matches.toList();
				List<DMatch> matches_final = new ArrayList<DMatch>();
				for (int i = 0; i < matchesList.size(); i++) {
					if (matchesList.get(i).distance <= min_dist) {
						matches_final.add(matches.toList().get(i));
					}
				}

				matches_final_mat = new MatOfDMatch();
				matches_final_mat.fromList(matches_final);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	//Delete imagepath
	public void deleteImage(String file_dj_path) {
     
        File fdelete = new File(file_dj_path);
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.e("-->", "file Deleted :" + file_dj_path);
                callBroadCast();
                String phoneNumber = sharedpreferences.getString("phoneNumber", "");
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.image_deleted), Toast.LENGTH_SHORT).show();
                if(phoneNumber!=null && !phoneNumber.equals("")){
                sendSMS();
                }else{
                	Toast.makeText(getApplicationContext(), getResources().getString(R.string.reg_phoneNumber), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("-->", "file not Deleted :" + file_dj_path);
            }
        }
    }

    public void callBroadCast() {
        if (Build.VERSION.SDK_INT >= 14) {
            Log.e("-->", " >= 14");
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStorageDirectory().toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                /*
                 *   (non-Javadoc)
                 * @see android.media.MediaScannerConnection.OnScanCompletedListener#onScanCompleted(java.lang.String, android.net.Uri)
                 */
                public void onScanCompleted(String path, Uri uri) {
                    Log.e("ExternalStorage", "Scanned " + path + ":");
                    Log.e("ExternalStorage", "-> uri=" + uri);
                }
            });
        } else {
            Log.e("-->", " < 14");
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }
    
    private void sendSMS() {
    	String phoneNumber = sharedpreferences.getString("phoneNumber", "");
    	String message= getResources().getString(R.string.sms_message);
        /*SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);*/
    	try{
    		System.out.println(phoneNumber);
    		System.out.println(message);
    	SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message); 
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
    	}catch(Exception e){
    		Log.e("SMS NOT SENT", e.toString());
    		System.out.println("MSG ERROR" + e.toString());
    	}
    }
}
