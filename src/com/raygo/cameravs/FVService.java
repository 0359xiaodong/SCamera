package com.raygo.cameravs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.Toast;

public class FVService extends Service 
{
	//���帡�����ڲ���
	private LinearLayout mFloatLayout;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;	
	private SurfaceView mFloatView;

	private Camera mCamera;
	private int swidth, sheight;
	private File savefoder;
	private Boolean AutoSave;
	private long SavePeroid;
	private int PictureWidth;
	private int PictureHeight;
	private Boolean isSaving;
	private String savedFile = null;
	private String savepath = null;
	
   
	@Override
	public void onCreate()
	{
		super.onCreate();
		isSaving = false;
		getParas();
		createFloatView();

		savepath = Environment.getExternalStorageDirectory() + "/SCamera/";
		savefoder = new File(savepath);
		if (!savefoder.exists()) {
			savefoder.mkdirs();
		}
		
		if(AutoSave){
			Timer timer = new Timer();
			timer.schedule(tPaizhao, 5000, SavePeroid);
		}	
	}
	
	//timer task to paizhao
	private TimerTask tPaizhao = new TimerTask() {
		@Override
		public void run() {
			if(mCamera != null){
				mCamera.takePicture(null, null, new TakePictureCallback());
			}
		}
	};
	
	
	private void getParas()
	{
		SharedPreferences settings = getSharedPreferences("LocalSettings", 0);
		AutoSave = settings.getBoolean("AutoSave",false);
		boolean isShow = settings.getBoolean("Preview",false);
		SavePeroid = (settings.getInt("SaveTime", 15))*1000;
		int SaveSize = settings.getInt("SaveSize", 0);
		int SavePath = settings.getInt("SavePath", 0);
		int ShowSize = settings.getInt("ShowSize", 2);
		
        switch(SaveSize)
        {
        case 0:
        	PictureWidth = 640;
        	PictureHeight = 480;
        	break;
        case 1:
        	PictureWidth = 800;
        	PictureHeight = 600;
        	break;
        case 2:
        	PictureWidth = 1024;
        	PictureHeight = 768;
        	break;
        default:
        	PictureWidth = 640;
        	PictureHeight = 480;
        	break;
        }
        
        switch(ShowSize)
        {
        case 0:
        	swidth = 300;
        	sheight = 450;
        	break;
        case 1:
        	swidth = 200;
        	sheight = 300;
        	break;
        case 2:
        	swidth = 100;
        	sheight = 150;
        	break;
        default:
        	swidth = 200;
        	sheight = 300;
        	break;
        }
        
        if(!isShow)
        {
        	swidth = 2;
        	sheight = 3;
        }
        
	}
	
	private void createFloatView()
	{
		wmParams = new WindowManager.LayoutParams();
		//��ȡWindowManagerImpl.CompatModeWrapper
		mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
		//����window type
		wmParams.type = LayoutParams.TYPE_SYSTEM_ALERT; 
		//����ͼƬ��ʽ��Ч��Ϊ����͸��
        wmParams.format = PixelFormat.RGBA_8888; 
        //���ø������ڲ��ɾ۽���ʵ�ֲ���������������������ɼ����ڵĲ�����
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL;// | LayoutParams.FLAG_NOT_TOUCHABLE;
        //������������ʾ��ͣ��λ��Ϊ����ö�
        wmParams.gravity = Gravity.CENTER | Gravity.TOP; 
        wmParams.x = 0;
        wmParams.y = 0;
        //�����������ڳ�������  
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        
        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //��ȡ����������ͼ���ڲ���
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_layout, null);
        //�ƶ�����
        mFloatLayout.setOnTouchListener(new OnTouchListener(){
        	int lastX, lastY;  
            int paramX, paramY;
			@Override
			public boolean onTouch(View mview, MotionEvent event) {
				// TODO Auto-generated method stub
	    		switch (event.getAction()) {
	    		case MotionEvent.ACTION_DOWN: //������ָ�������¶���
	    			lastX = (int) event.getRawX();
	                lastY = (int) event.getRawY();  
	                paramX = wmParams.x;  
	                paramY = wmParams.y;
	    			break;
	    		case MotionEvent.ACTION_MOVE:   //������ָ�����ƶ�����           
	    			int dx = (int) event.getRawX() - lastX;  
	                int dy = (int) event.getRawY() - lastY;  
	                wmParams.x = paramX + dx;  
	                wmParams.y = paramY + dy;
	    			mWindowManager.updateViewLayout(mFloatLayout, wmParams);  //ˢ����ʾ
	    			break;
	    		}
				return true;
			}});
        //���mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED), 
        					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        //�������ڰ�ť
        mFloatView = (SurfaceView)mFloatLayout.findViewById(R.id.sfview);

        mFloatView.getHolder().setFixedSize(swidth, sheight);
        mFloatView.getHolder().addCallback(new SurfaceCallback());

	}
		
    //Save the pic
	private final class TakePictureCallback implements PictureCallback {
		
    	public void onPictureTaken(byte[] data, Camera camera) {
        	try {
    			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);    			
    			
    			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
    	        String date = dateFormat.format(new Date());
    	        String photoFile = date + ".png";
    			File file = new File(savefoder, photoFile);
    			
    			FileOutputStream outStream = new FileOutputStream(file);
    			bitmap.compress(CompressFormat.PNG, 100, outStream);
    			outStream.close();
    			
    			mCamera.stopPreview();
    			mCamera.startPreview();
    			savedFile = photoFile;
				isSaving = true;
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }
    };
    
    //Start the camera
    private final class SurfaceCallback implements SurfaceHolder.Callback {
    	
    	public void surfaceCreated(SurfaceHolder holder) { 
    		mCamera = Camera.open();
    		if(mCamera != null){
    			try {
    				mCamera.setPreviewDisplay(holder);
    			} catch (IOException e) {
    				Log.e("Save Error:", e.toString());
    				mCamera = null;
    			}
    		}
    		else
    		{
    			Toast.makeText(FVService.this, "Open camera fail", Toast.LENGTH_SHORT).show();
    		}
    	} 
    	
    	public void surfaceDestroyed(SurfaceHolder holder) { 
    		if (mCamera != null) {
    			mCamera.setPreviewCallback(null);
    			mCamera.stopPreview();
    			mCamera.release();
    			mCamera = null;
			}
    	} 

    	public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) { 
    		if (mCamera != null) {
    			// ����������
    			Camera.Parameters parameters = mCamera.getParameters();
    			// ��ת90��
				setDisplayOrientation(mCamera, 90);
				// ����Ԥ����С
				//parameters.setPreviewSize(width, height);
				// ������Ƭ��С
				parameters.setPictureSize(PictureWidth, PictureHeight);
				// ������Ƭ��ʽ
				parameters.setPictureFormat(PixelFormat.JPEG);
				// �����������
				mCamera.setParameters(parameters);				
				// ��ʼԤ��
				mCamera.startPreview();
				Toast.makeText(FVService.this, "Start service success.", Toast.LENGTH_SHORT).show();
    		}
    	}
    	
    	protected void setDisplayOrientation(Camera camera, int angle) {
    		Method downPolymorphic;
    		try {
    			downPolymorphic = camera.getClass().getMethod(
    					"setDisplayOrientation", new Class[] { int.class });
    			if (downPolymorphic != null)
    				downPolymorphic.invoke(camera, new Object[] { angle });
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }

	@Override
	public IBinder onBind(Intent intent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() 
	{
		// TODO Auto-generated method stub
		super.onDestroy();
		isSaving = false;
		if(mFloatLayout != null)
		{
			mWindowManager.removeView(mFloatLayout);
		}
	}
	
}
