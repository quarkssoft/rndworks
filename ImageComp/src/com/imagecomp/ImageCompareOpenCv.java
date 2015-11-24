package com.imagecomp;


import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ImageCompareOpenCv  extends Activity{
	private static final String TAG = "OCVSample::Activity";
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

	public ImageCompareOpenCv() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

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

	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		
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
		imageSelection(ResourceToUri(getApplicationContext(), R.drawable.ic_launcher),2);
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
	        		new asyncTask(ImageCompareOpenCv.this).execute();
	        	}
	        	else if(compare==0)
	        		Toast.makeText(ImageCompareOpenCv.this, "Images are exact", Toast.LENGTH_LONG).show();
	        	else
	        		Toast.makeText(ImageCompareOpenCv.this, "Compared images are duplicates", Toast.LENGTH_LONG).show();
	        		
			startTime = System.currentTimeMillis();
		} else
			Toast.makeText(ImageCompareOpenCv.this,
					
					"You haven't selected images.", Toast.LENGTH_LONG)
					.show();
	
	}

	@Override
	protected void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);
		min_dist = newIntent.getExtras().getInt("min_dist");
		descriptor = newIntent.getExtras().getInt("descriptor");
		min_matches = newIntent.getExtras().getInt("min_matches");
		run();
	}
	public static Uri ResourceToUri (Context context,int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID) );
    }

	@Override
	public void onPause() {
		super.onPause();
		
		
	}

	@Override
	public void onResume() {
		super.onResume();
		/*OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this,
				mLoaderCallback);
*/	}
	

	
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
	
	////////////////////////////////////////
	public static class asyncTask extends AsyncTask<Void, Void, Void> {
		private static Mat img1, img2, descriptors, dupDescriptors;
		private static FeatureDetector detector;
		private static DescriptorExtractor DescExtractor;
		private static DescriptorMatcher matcher;
		private static MatOfKeyPoint keypoints, dupKeypoints;
		private static MatOfDMatch matches, matches_final_mat;
		//private static ProgressDialog pd;
		private static boolean isDuplicate = false;
		private ImageCompareOpenCv asyncTaskContext=null;
		private static Scalar RED = new Scalar(255,0,0);
		private static Scalar GREEN = new Scalar(0,255,0);
		public asyncTask(ImageCompareOpenCv context)
		{
			asyncTaskContext=context;
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
					Toast.makeText(asyncTaskContext, "Images are simalar", Toast.LENGTH_LONG).show();
				} else {
					text = finalMatchesList.size()
							+ " matches were found. Images aren't similar.\nTime taken="
							+ (endTime - startTime) + "ms";
					isDuplicate = false;
					Toast.makeText(asyncTaskContext, "Images are'nt similar", Toast.LENGTH_LONG).show();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				Toast.makeText(asyncTaskContext, e.toString(),
						Toast.LENGTH_LONG).show();
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
}
