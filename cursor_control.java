import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.Videoio;
import org.opencv.videoio.VideoCapture;
import org.opencv.utils.Converters;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;



public class video extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JFrame frame=new JFrame("Hand track");
	private JLabel lab = new JLabel();
	private static String string="Waiting for action";

	private static Point last=new Point();
	private static boolean close=false;
	private static boolean act=false;
	private static long current=0;
	private static long prev=0;
	private static boolean start=false;
	/**
	 * Create the panel.
	 */
	public video() {

	}

	public void setframe(final VideoCapture webcam){
		frame.setSize(1024,768);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.getContentPane().add(lab);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Closed");
				close=true;
				webcam.release();
				e.getWindow().dispose();
			}
		});
	}

	public void frametolabel(Mat matframe){
		MatOfByte cc=new MatOfByte();
		Imgcodecs.imencode(".JPG", matframe, cc);
		byte[] ch= cc.toArray();
		InputStream ss=new ByteArrayInputStream(ch);
		try {
			BufferedImage aa= ImageIO.read(ss);
			lab.setIcon(new ImageIcon(aa));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double calculateDistance(Point P1,Point P2){
		double distance= Math.sqrt(((P1.x-P2.x)*(P1.x-P2.x))+((P1.y-P2.y)*(P1.y-P2.y)));

		return distance;
	}

	public double calculateAngle(Point P1,Point P2,Point P3){
		double angle=0;
		Point v1=new Point();
		Point v2=new Point();
		v1.x=P3.x-P1.x;
		v1.y=P3.y-P1.y;
		v2.x=P3.x-P2.x;
		v2.y=P3.y-P2.y;
		double dotproduct = (v1.x*v2.x) + (v1.y*v2.y); 
		double length1 = Math.sqrt((v1.x*v1.x)+(v1.y*v1.y));
		double length2 = Math.sqrt((v2.x*v2.x)+(v2.y*v2.y));
		double angle1 = Math.acos(dotproduct/(length1*length2));
		angle=angle1*180/Math.PI;

		return angle;
	}
	public Mat filtrColorRgb(int b,int g,int r,int b1,int g1,int r1,Mat image){
		Mat modified=new Mat();
		if(image!=null){
			Core.inRange(image, new Scalar(b,g,r), new Scalar(b1,g1,r1), modified);
		}
		else{
			System.out.println("Error image");
		}
		return modified;
	}

	public Mat filtrColorHsv(int h,int s,int v,int h1,int s1,int v1,Mat image){
		Mat modified=new Mat();
		if(image!=null){
			Core.inRange(image, new Scalar(h,s,v), new Scalar(h1,s1,v1), modified);
		}
		else{
			System.out.println("Error image");
		}
		return modified;
	}

	public Mat skindetection(Mat orig){
		Mat mask=new Mat();
		Mat result=new Mat();
		Core.inRange(orig, new Scalar(0,0,0),new Scalar(30,30,30),result);
		Imgproc.cvtColor(orig, mask, Imgproc.COLOR_BGR2HSV);
		for(int i=0;i<mask.size().height;i++){
			for(int j=0;j<mask.size().width;j++){
				if(mask.get(i,j)[0]<19 || mask.get(i, j)[0]>150
						&& mask.get(i,j)[1]>25 && mask.get(i,j)[1]<220){

					result.put(i,j,255,255,255);

				}
				else{
					result.put(i, j, 0,0,0);
				}
			}

		}


		return result;

	}

	public Mat filtrMorph(int kd,int ke,Mat image){
		Mat modified=new Mat();
		Imgproc.erode(image, modified, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(ke,ke)));
		//Imgproc.erode(modified, modified, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(ke,ke)));
		Imgproc.dilate(modified, modified,  Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(kd,kd)));
		return modified;

	}

	public List<MatOfPoint> searchContours(Mat original, Mat image,boolean draws, boolean drawAll, int filtrPixel){
		List<MatOfPoint> contours = new LinkedList<MatOfPoint>();
		List<MatOfPoint> contoursbig = new LinkedList<MatOfPoint>();
		Mat hierarchy= new Mat();

		Imgproc.findContours(image,contours , hierarchy ,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));

		for(int i=0;i<contours.size();i++) {
			if(contours.get(i).size().height>filtrPixel){
				contoursbig.add(contours.get(i));
				if(draws && !drawAll)
					Imgproc.drawContours(original, contours,i,new Scalar(0,255,0),2,8,hierarchy,0,new Point());
			}

			if(drawAll && !draws)
				Imgproc.drawContours(original, contours,i,new Scalar(0,255,255),2,8,hierarchy,0,new Point());

		}
		return contoursbig;
	}

	public List<Point> listContours(Mat image,int filtrPixel){
		List<MatOfPoint> contours = new LinkedList<MatOfPoint>();
		List<MatOfPoint> contoursbig = new LinkedList<MatOfPoint>();
		List<Point> listPoints=new LinkedList<Point>();
		Mat hierarchy= new Mat();

		Imgproc.findContours(image,contours , hierarchy ,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));

		for(int i=0;i<contours.size();i++) {
			//System.out.println("Dimension contour"+contours.get(i).size().height);
			if(contours.get(i).size().height>filtrPixel){
				contoursbig.add(contours.get(i));
			}

		}
		if(contoursbig.size()>0){

			listPoints=contoursbig.get(0).toList();

		}
		return listPoints;
	}

	public List<Point> envelopDefects(Mat image,List<MatOfPoint> contours,boolean draws,int thresholdDepth){
		List<Point> defects=new LinkedList<Point>();

		for(int i=0;i<contours.size();i++){
			MatOfInt hull_=new MatOfInt();
			MatOfInt4 convexityDefects=new MatOfInt4();

			@SuppressWarnings("unused")
			List<Point> contourPoints = new LinkedList<Point>();
			contourPoints=contours.get(i).toList();

			Imgproc.convexHull(contours.get(i), hull_);

			if (hull_.size().height>=4){


				Imgproc.convexityDefects(contours.get(i), hull_, convexityDefects);

				List<Point> pts=new ArrayList<Point>();
				MatOfPoint2f pr=new MatOfPoint2f();
				Converters.Mat_to_vector_Point(contours.get(i), pts);
				//rettangle
				pr.create((int)(pts.size()), 1, CvType.CV_32S);
				pr.fromList(pts);
				if(pr.height()>10){
					RotatedRect r=Imgproc.minAreaRect(pr);
					Point[] rect=new Point[4];
					r.points(rect);

					Imgproc.line(image, rect[0], rect[1],new Scalar(0,100,0),2);
					Imgproc.line(image, rect[0], rect[3],new Scalar(0,100,0),2);
					Imgproc.line(image, rect[1], rect[2],new Scalar(0,100,0),2);
					Imgproc.line(image, rect[2], rect[3],new Scalar(0,100,0),2);
					Imgproc.rectangle(image, r.boundingRect().tl(), r.boundingRect().br(), new Scalar(50,50,50));				
				}
				//fine rettangle

				int[] buff = new int[4];
				int[] zx=new int[1];
				int[] zxx=new int[1];
				for(int i1=0;i1<hull_.size().height;i1++){
					if(i1<hull_.size().height-1){
						hull_.get(i1,0,zx);
						hull_.get(i1+1,0,zxx);
					}
					else
					{
						hull_.get(i1,0,zx);
						hull_.get(0,0,zxx);
					}
					if(draws)
						Imgproc.line(image, pts.get(zx[0]), pts.get(zxx[0]), new Scalar(0,140,140),2);
				}


				for(int i1=0;i1<convexityDefects.size().height;i1++){
					convexityDefects.get(i1, 0,buff);
					if(buff[3]/256>thresholdDepth){
						if(pts.get(buff[2]).x>0 && pts.get(buff[2]).x<1024 && pts.get(buff[2]).y>0 && pts.get(buff[2]).y<768){
							defects.add(pts.get(buff[2]));
							Imgproc.circle(image, pts.get(buff[2]), 6, new Scalar(0,255,0));
							if(draws)
								Imgproc.circle(image, pts.get(buff[2]), 6, new Scalar(0,255,0));

						}
					}
				}
				if (defects.size()<3){
					int dim=pts.size();
					Imgproc.circle(image, pts.get(0), 3, new Scalar(0,255,0),2);
					Imgproc.circle(image, pts.get(0+dim/4), 3, new Scalar(0,255,0),2);
					defects.add(pts.get(0));
					defects.add(pts.get(0+dim/4));


				}
			}
		}
		return defects;
	}

	public Point centerPalm(Mat image,List<Point> defects1){
		MatOfPoint2f pr=new MatOfPoint2f();
		Point center=new Point();
		float[] radius=new float[1];
		pr.create((int)(defects1.size()), 1, CvType.CV_32S);
		pr.fromList(defects1);

		if(pr.size().height>0){
			start=true;
			Imgproc.minEnclosingCircle(pr, center, radius);

			//Core.circle(image, center,(int) radius[0], new Scalar(255,0,0));
			//  Core.circle(image, center, 3, new Scalar(0,0,255),4);
		}
		else{
			start=false;
		}
		return center;

	}

	public List<Point> fingers(Mat image,List<Point> contourPoints,Point center){
		List<Point> fingerPoints=new LinkedList<Point>();
		List<Point> fingers=new LinkedList<Point>();
		int interval=55;
		for(int j=0;j<contourPoints.size();j++){
			Point prec=new Point();
			Point vertice=new Point();
			Point next=new Point();
			vertice=contourPoints.get(j);
			if(j-interval>0){

				prec=contourPoints.get(j-interval);
			}
			else{
				int a=interval-j;
				prec=contourPoints.get(contourPoints.size()-a-1);
			}
			if(j+interval<contourPoints.size()){
				next=contourPoints.get(j+interval);
			}
			else{
				int a=j+interval-contourPoints.size();
				next=contourPoints.get(a);
			}

			Point v1= new Point();
			Point v2= new Point();
			v1.x=vertice.x-next.x;
			v1.y=vertice.y-next.y;
			v2.x=vertice.x-prec.x;
			v2.y=vertice.y-prec.y;
			double dotproduct = (v1.x*v2.x) + (v1.y*v2.y); 
			double length1 = Math.sqrt((v1.x*v1.x)+(v1.y*v1.y));
			double length2 = Math.sqrt((v2.x*v2.x)+(v2.y*v2.y));
			double angle = Math.acos(dotproduct/(length1*length2));
			angle=angle*180/Math.PI;
			if(angle<60)
			{
				double centerprec=Math.sqrt(((prec.x-center.x)*(prec.x-center.x))+((prec.y-center.y)*(prec.y-center.y)));
				double centervert=Math.sqrt(((vertice.x-center.x)*(vertice.x-center.x))+((vertice.y-center.y)*(vertice.y-center.y)));
				double centernext=Math.sqrt(((next.x-center.x)*(next.x-center.x))+((next.y-center.y)*(next.y-center.y)));
				if(centerprec<centervert && centernext<centervert){

					fingerPoints.add(vertice);
					//Core.circle(image, vertice, 2, new Scalar(200,0,230));

					//Core.line(image, vertice, center, new Scalar(0,255,255));
				}
			}
		}

		Point media=new Point();
		media.x=0;
		media.y=0;
		int med=0;
		boolean t=false;
		if(fingerPoints.size()>0){
			double dif=Math.sqrt(((fingerPoints.get(0).x-fingerPoints.get(fingerPoints.size()-1).x)*(fingerPoints.get(0).x-fingerPoints.get(fingerPoints.size()-1).x))+((fingerPoints.get(0).y-fingerPoints.get(fingerPoints.size()-1).y)*(fingerPoints.get(0).y-fingerPoints.get(fingerPoints.size()-1).y)));
			if(dif<=20){
				t=true;
			}
		}
		for(int i=0;i<fingerPoints.size()-1;i++){

			double d=Math.sqrt(((fingerPoints.get(i).x-fingerPoints.get(i+1).x)*(fingerPoints.get(i).x-fingerPoints.get(i+1).x))+((fingerPoints.get(i).y-fingerPoints.get(i+1).y)*(fingerPoints.get(i).y-fingerPoints.get(i+1).y)));

			if(d>20 || i+1==fingerPoints.size()-1){
				Point p=new Point();

				p.x=(int)(media.x/med);
				p.y=(int)(media.y/med);

				//if(p.x>0 && p.x<1024 && p.y<768 && p.y>0){

					fingers.add(p);
				//}

				if(t && i+1==fingerPoints.size()-1){
					Point ult=new Point();
					if(fingers.size()>1){
						ult.x=(fingers.get(0).x+fingers.get(fingers.size()-1).x)/2;
						ult.y=(fingers.get(0).y+fingers.get(fingers.size()-1).y)/2;
						fingers.set(0, ult);
						fingers.remove(fingers.size()-1);
					}
				}
				med=0;
				media.x=0;
				media.y=0;
			}
			else{

				media.x=(media.x+fingerPoints.get(i).x);
				media.y=(media.y+fingerPoints.get(i).y);
				med++;


			}
		}


		return fingers;
	}

	public void drawFingerCenterPalm(Mat image,Point center,Point finger,List<Point> fingers){

		Imgproc.line(image,new Point(150,50),new Point(730,50), new Scalar(255,0,0),2);
		Imgproc.line(image,new Point(150,380),new Point(730,380), new Scalar(255,0,0),2);
		Imgproc.line(image,new Point(150,50),new Point(150,380), new Scalar(255,0,0),2);
		Imgproc.line(image,new Point(730,50),new Point(730,380), new Scalar(255,0,0),2);
		if(fingers.size()==1){
			Imgproc.line(image, center, finger, new Scalar(0, 255, 255),4);
			Imgproc.circle(image, finger, 3, new Scalar(255,0,255),3);
			//Core.putText(image, finger.toString(), finger, Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0,200,255));

		}
		else
		{
			for(int i=0;i<fingers.size();i++){
				Imgproc.line(image, center, fingers.get(i), new Scalar(0, 255, 255),4);
				Imgproc.circle(image, fingers.get(i), 3, new Scalar(255,0,255),3);
			}
		}
		Imgproc.circle(image, center, 3, new Scalar(0,0,255),3);
		//Core.putText(image, center.toString(), center, Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0,200,255));

	}

	public void mousetrack(List<Point> fingers,Point finger,Point center,Robot r,boolean on,Mat image, long temp) throws InterruptedException{

		if(on && center.x>10 && center.y>10 && finger.x>10 && center.y>10 && start){
			current=temp;
			switch(fingers.size()){
			case 0: 
				if(act && current-prev>500){
					string="Drag & drop";
					r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					act=false;
				}
				else{
					if(current-prev>500){
						Point p=new Point();
						Point np=new Point();
						np.x=center.x-last.x;
						np.y=center.y-last.y;
						p.x=(int)(-1*(np.x-730))*1366/580;
						p.y=(int)(np.y-50)*768/330;
						if(p.x>0 && p.x>0 && p.x<1367 && p.y<769){
							r.mouseMove((int)p.x,(int)p.y);
						}
						
					}
				}
				break;
			case 1: 
				

				if(act && current-prev>500){
					string="Click";
					System.out.println("click");
					r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					
				
					r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					
					r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					System.out.println("release");

					act=false;
				}
				else{
					if(current-prev>500){
						string="Pointer";

						Point p1=new Point();
						p1.x=(int)(-1*(finger.x-730))*1366/580;
						p1.y=(int)(finger.y-50)*768/330;
						if(p1.x>0 && p1.x>0 && p1.x<1367 && p1.y<769){
							r.mouseMove((int)p1.x,(int)p1.y);
						}
						last.x=center.x-finger.x;
						last.y=center.y-finger.y;
					}
				}
				break;
			case 2: 
				double angle=calculateAngle(fingers.get(0),fingers.get(1),center);
				r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
				if(act && current-prev>500){
					act=false;
					if((int)angle<30){
						string="Double click";
						System.out.println("double click");
						r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
						r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
						r.delay(100);
						r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
						r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					}
					else{
						string="Right Click";
						r.mousePress(InputEvent.BUTTON3_DOWN_MASK);
						r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					}
					
				}
				break;
			case 3:
				string="Cancels";
				act=false;
				break;
			case 4:string="Block Pointer";
			r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

			prev=temp;
			act=true;

			break;
				
			case 5: string="Block Pointer";
			if(!act){
			r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

			prev=temp;
			act=true;
			}
			break;
			default: string="Waiting for action!";
			
			break;
			}

		}
		else{
			r.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		Imgproc.putText(image,string,new Point(50,40), Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(200,0,0));

	}

	public Point filtrMediaMobile(List<Point> buffer, Point current){
		Point media=new Point();
		media.x=0;
		media.y=0;
		for(int i=buffer.size()-1;i>0;i--){
			buffer.set(i, buffer.get(i-1));
			media.x=media.x+buffer.get(i).x;
			media.y=media.y+buffer.get(i).y;
		}
		buffer.set(0, current);
		media.x=(media.x+buffer.get(0).x)/buffer.size();
		media.y=(media.y+buffer.get(0).y)/buffer.size();
		return media;
	}



	public static void main(String[] args) throws InterruptedException, AWTException {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		video v=new video();
		VideoCapture webcam=new VideoCapture(0);
		//webcam.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT,720);
		//webcam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,1280);
		v.setframe(webcam);
		Robot r=new Robot();
		Mat frame = new Mat();
		Mat modified=new Mat();
		Point center=new Point();
		Point finger=new Point();
		List<Point> buffer=new LinkedList<Point>();
		List<Point> bufferfingers=new LinkedList<Point>();
		List<Point> fingers=new LinkedList<Point>();
		long temp=0;


		while(true && !close){

			if(!webcam.isOpened() && !close){
				System.out.println("Camera Error");
			}
			else{
				List<Point> defect=new LinkedList<Point>();
				if(!close){
					temp=System.currentTimeMillis();
					webcam.retrieve(frame);
					//modified = v.filtrMorph(2, 7, v.filtrColorRgb(0, 0, 0, 40, 40, 40, frame));
					modified = v.filtrMorph(2, 7, v.filtrColorHsv(0, 0, 0, 180, 255, 40,frame));
			
					defect=v.envelopDefects(frame,v.searchContours(frame, modified, false, false, 450), false, 5);

					if(buffer.size()<7){
						buffer.add(v.centerPalm(frame,defect));
					}
					else
					{
						center=v.filtrMediaMobile(buffer, v.centerPalm(frame,defect));
						//System.out.println((int)center.x+"         "+(int)center.y+"       "+(int)v.centerPalm(frame,defect).x+"        "+(int)v.centerPalm(frame,defect).y);
					}

					fingers=v.fingers(frame, v.listContours(modified, 200), center);

					if(fingers.size()==1 && bufferfingers.size()<5){
						bufferfingers.add(fingers.get(0));
						finger=fingers.get(0);
					}
					else
					{
						if(fingers.size()==1){
							finger=v.filtrMediaMobile(bufferfingers, fingers.get(0)); 
							//System.out.println((int)finger.x +"           "+(int)finger.y+"           "+(int)fingers.get(0).x+"           "+(int)fingers.get(0).y);
						}
					}

					v.drawFingerCenterPalm(frame, center, finger, fingers);



				v.mousetrack(fingers,finger,center, r,true,frame,temp);

					v.frametolabel(frame);

				}
			}

		}


	}
}



