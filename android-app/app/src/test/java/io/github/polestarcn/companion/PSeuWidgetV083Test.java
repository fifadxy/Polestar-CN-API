package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.text.*;
import android.text.style.StyleSpan;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PSeuWidgetV083Test {
 private Context c;private VehicleSnapshot vehicle;
 @Before public void start(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("preferences",0).edit().clear().commit();AppState.setRegion(c,Region.CHINA);long now=System.currentTimeMillis();vehicle=new VehicleSnapshot("Polestar 2","masked",63,230,65845,"空闲","全部关闭","全部关闭","已关闭","已上锁",now,"LIVE",false,"开启","关闭").withHealth(new VehicleHealthSnapshot(1,1,1,now,1,1,1,-1,1,931,75,14959)).withMileage(new MileageSnapshot(65845000L,100.5,25.2,45.0,35.0,18.5,21.5,now,now));}
 private View render(String style,WidgetRenderer.Size size,int w,int h){AppPreferences.set(c,"widget_design",style);RemoteViews remote=WidgetRenderer.createViews(c,83,size,w,h,null,vehicle);View root=remote.apply(c,new FrameLayout(c));root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);return root;}
 private void fit(View v){
  if(v.getVisibility()!=View.VISIBLE)return;
  if(v instanceof TextView){TextView t=(TextView)v;if(t.length()>0&&t.getLayout()!=null)assertTrue(t.getText()+" height "+t.getLayout().getHeight()+" / "+t.getHeight(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());}
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)fit(((ViewGroup)v).getChildAt(i));
 }
 @Test public void everyWidgetStyleFitsFiveSizesSystemFontAndBold()throws Exception{
  for(float system:new float[]{1f,1.3f}){Configuration config=new Configuration(c.getResources().getConfiguration());config.fontScale=system;android.util.DisplayMetrics metrics=c.getResources().getDisplayMetrics();metrics.scaledDensity=metrics.density*system;c.getResources().updateConfiguration(config,metrics);
   for(String bold:new String[]{"off","on"})for(String font:new String[]{"80","120"})for(String style:new String[]{VoLikeWidgetRenderer.CLASSIC,VoLikeWidgetRenderer.SIMPLE,VoLikeWidgetRenderer.DETAILED,VoLikeWidgetRenderer.POLESTAR,VoLikeWidgetRenderer.PSEU})for(WidgetRenderer.Size size:WidgetRenderer.Size.values()){
    AppPreferences.set(c,"widget_bold",bold);AppPreferences.set(c,"widget_font_percent",font);int w=size==WidgetRenderer.Size.MINI?118:size==WidgetRenderer.Size.SQUARE?150:300,h=size==WidgetRenderer.Size.MINI?88:size==WidgetRenderer.Size.WIDE?70:size==WidgetRenderer.Size.DATA?260:170;
    View root=render(style,size,w,h);try{fit(root);}catch(AssertionError e){throw new AssertionError(style+" "+size+" font="+system+" user="+font+" bold="+bold,e);}
    assertTrue(root.findViewById(R.id.widget_action_lock).getHeight()>=48);assertTrue(root.findViewById(R.id.widget_action_climate).getHeight()>=48);assertTrue(root.findViewById(R.id.widget_action_lock).getWidth()>=48);assertTrue(root.findViewById(R.id.widget_action_climate).getWidth()>=48);assertFalse(((TextView)root.findViewById(R.id.widget_region)).getText().toString().isEmpty());
   }
  }
 }
 @Test public void pseuSupportsCustomFieldsAndFourTypesAtMultipleAspectRatios()throws Exception{
  AppPreferences.set(c,"widget_bold","on");AppPreferences.set(c,"widget_opacity","100");
  for(String type:new String[]{"battery","range","car","controls"}){AppPreferences.set(c,"pseu_type",type);for(int[] frame:new int[][]{{300,170},{480,150},{220,240},{420,280}}){
   View root=render(VoLikeWidgetRenderer.PSEU,WidgetRenderer.Size.LARGE,frame[0],frame[1]);fit(root);TextView first=root.findViewById(R.id.widget_metric0);assertTrue(first.getText() instanceof Spanned);assertTrue(((Spanned)first.getText()).getSpans(0,first.length(),StyleSpan.class).length>0);
   ImageView car=root.findViewById(R.id.widget_car);assertEquals(ImageView.ScaleType.FIT_CENTER,car.getScaleType());assertEquals(type.equals("car")?View.VISIBLE:View.GONE,car.getVisibility());
   if(frame[0]==300){File file=new File("build/reports/previews/v083/pseu-"+type+".png");file.getParentFile().mkdirs();Bitmap image=Bitmap.createBitmap(600,340,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(2,2);root.draw(canvas);try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
  }}
  AppPreferences.set(c,WidgetRenderer.selectionKey(c,WidgetRenderer.Size.LARGE),"odometer,ta_distance,service_km_left");View custom=render(VoLikeWidgetRenderer.PSEU,WidgetRenderer.Size.LARGE,300,170);assertTrue(((TextView)custom.findViewById(R.id.widget_metric0)).getContentDescription().toString().contains("65845"));
 }
 @Test public void boldToggleAffectsNativeTextInAllDesigns(){
  for(String style:new String[]{VoLikeWidgetRenderer.CLASSIC,VoLikeWidgetRenderer.SIMPLE,VoLikeWidgetRenderer.DETAILED,VoLikeWidgetRenderer.POLESTAR,VoLikeWidgetRenderer.PSEU})for(String bold:new String[]{"off","on"}){AppPreferences.set(c,"widget_bold",bold);View root=render(style,WidgetRenderer.Size.LARGE,300,170);TextView first=root.findViewById(R.id.widget_metric0);Spanned text=(Spanned)first.getText();assertEquals(bold.equals("on")?1:0,text.getSpans(0,text.length(),StyleSpan.class).length);}
 }
 @Test public void shortLandscapeAndResizedWideWidgetsChangeLayoutWithoutStretching(){
  for(String style:new String[]{VoLikeWidgetRenderer.CLASSIC,VoLikeWidgetRenderer.SIMPLE,VoLikeWidgetRenderer.PSEU})for(WidgetRenderer.Size size:WidgetRenderer.Size.values())for(int[] frame:new int[][]{{269,51},{420,90},{300,170},{200,220}}){
   View root=render(style,size,frame[0],frame[1]);try{fit(root);}catch(AssertionError e){throw new AssertionError(style+" "+size+" "+frame[0]+"x"+frame[1],e);}assertTrue(root.findViewById(R.id.widget_action_lock).getHeight()>=48);assertTrue(root.findViewById(R.id.widget_action_climate).getHeight()>=48);
  }
 }
 @Test public void vehicleCardRetainsVisibleTitleAndTimestamp(){
  AppPreferences.set(c,"pseu_type","car");View root=render(VoLikeWidgetRenderer.PSEU,WidgetRenderer.Size.LARGE,300,170);
  for(int id:new int[]{R.id.widget_card_header,R.id.widget_title,R.id.widget_region,R.id.widget_refresh_row}){View view=root.findViewById(id);assertEquals(View.VISIBLE,view.getVisibility());assertTrue(view.getHeight()>0);}
  try{File file=new File("build/reports/previews/v083/pseu-car.png");file.getParentFile().mkdirs();Bitmap image=Bitmap.createBitmap(600,340,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(2,2);root.draw(canvas);try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}catch(IOException e){throw new AssertionError(e);}
 }
 @Test public void expandedControlsRetainCorrectFixedGlyphs(){
  AppPreferences.set(c,"pseu_type","controls");View controls=render(VoLikeWidgetRenderer.PSEU,WidgetRenderer.Size.LARGE,300,170);
  assertEquals(R.drawable.ic_lock,Shadows.shadowOf(((ImageView)controls.findViewById(R.id.widget_lock_icon)).getDrawable()).getCreatedFromResId());
  assertEquals(R.drawable.ic_unlock,Shadows.shadowOf(((ImageView)controls.findViewById(R.id.widget_climate_icon)).getDrawable()).getCreatedFromResId());
  try{File file=new File("build/reports/previews/v083/pseu-controls.png");file.getParentFile().mkdirs();Bitmap image=Bitmap.createBitmap(600,340,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);canvas.scale(2,2);controls.draw(canvas);try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}catch(IOException e){throw new AssertionError(e);}
 }
 @Test public void backgroundsFollowTheRequestedShapeWithoutEncodingText(){Bitmap wide=WidgetRenderer.background(c,WidgetRenderer.Size.LARGE,400,120,VoLikeWidgetRenderer.PSEU);Bitmap square=WidgetRenderer.background(c,WidgetRenderer.Size.LARGE,200,200,VoLikeWidgetRenderer.PSEU);assertEquals(400,wide.getWidth());assertEquals(120,wide.getHeight());assertEquals(200,square.getWidth());assertEquals(200,square.getHeight());assertNotSame(wide,square);}
}
