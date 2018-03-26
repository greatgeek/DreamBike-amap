package com.dreambike.amaptest;

import android.Manifest;
import android.content.pm.PackageManager;

import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.*;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.api.services.core.*;
import com.dreambike.amaptest.dialog.LoadDialog;
import com.dreambike.amaptest.dialog.MyLoadDialog;
import com.dreambike.amaptest.overlay.WalkRouteOverlay;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity implements AMap.OnMarkerClickListener
,AMap.OnMyLocationChangeListener,AMap.InfoWindowAdapter,RouteSearch.OnRouteSearchListener{
    MapView mMapView=null;
    MyLocationStyle myLocationStyle;
    LatLonPoint mStartPoint;//我的位置坐标
    LatLonPoint mEndPoint;//终点坐标
    AMap aMap;

    String markerlocjsonData=null;
    boolean isdone=false;
    boolean isFirstLocat=true;
    private ImageView refresh;
    private static final String TAG = "MainActivity";

    private RouteSearch mRouteSearch;
    private final int ROUTE_TYPE_WALK=3;
    private WalkRouteResult mWalkRouteResult;
    private WalkRouteOverlay mWalkRouteOverlay;
    private Marker tempMarker;/**用于点击显示点的傀儡Marker**/
    private String timeMin;
    private String timeSec;
    private String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        refresh=(ImageView)findViewById(R.id.iv_refresh);//获取ImaageView实例
        mMapView=(MapView)findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        final Animation circle_anim= AnimationUtils.loadAnimation(this,R.anim.refresh_rotate);//获取旋转资源
        LinearInterpolator interpolator=new LinearInterpolator();//设置匀速旋转
        circle_anim.setInterpolator(interpolator);//旋转资源设置为匀速旋转

        refresh.startAnimation(circle_anim);/**进行旋转**/
        refresh.setOnClickListener(new View.OnClickListener() {//图标点击事件
            @Override
            public void onClick(View v) {
                new GetMarker().execute();//点击刷新图标后，联网刷新单车数据
                refresh.startAnimation(circle_anim);//同时图标开始转动
                MyshowDialog();
            }
        });

        init();
        List<String> permissionList=new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_PHONE_STATE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissionList.isEmpty()){
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else{
            Mylocation();
        }
        new GetMarker().execute();
        MyshowDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0){
                    for (int result:grantResults){
                        if (result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    new GetMarker().execute();
                }else{
                    Toast.makeText(this, "发生未知错误！", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }


    /*******************************function define******************************************************8*/
    /**初始化AMap对象**/
    private void init(){
        if (aMap==null){
            aMap=mMapView.getMap();
            aMap.moveCamera(CameraUpdateFactory.zoomTo(17f));//高德地图初始化后设置地图的毕缩放级别
            aMap.getUiSettings().setZoomControlsEnabled(false);//取消缩放按钮
            mRouteSearch=new RouteSearch(this);
            mRouteSearch.setRouteSearchListener(this);
        }
        registerListener();
    }
    /**注册监听**/
    private void registerListener(){
        aMap.setOnMarkerClickListener(this);
        mMapView.getMap().setOnMyLocationChangeListener(this);//非常重要，设置此监听器即可监听变化，加入控制事件
        aMap.setInfoWindowAdapter(this);
    }

    /***控制事件*/
    void Mylocation(){//
        if(isFirstLocat) {
            myLocationStyle = new MyLocationStyle();
            myLocationStyle.interval(2000)
                    .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                    .showMyLocation(true);
            aMap.setMyLocationStyle(myLocationStyle);
            aMap.setMyLocationEnabled(true);

            isFirstLocat=false;
        }
        if (isdone){//若第一次isdone为false，说明单车数据未准备好
            ArrayList<MarkerOptions> markerOptions=parseJSONWithJSONObject(markerlocjsonData);
            aMap.clear(true);
            aMap.addMarkers(markerOptions,true);
            isdone=false;//进入了一次后便不需要再一次进入，只需要初始化一次便可，所以将其设置为false
            refresh.clearAnimation();//数据加载完成后，停止转动
            MyLoadDialog.getInstance().dismiss();
        }

    }
    private void showDialog(){
        LoadDialog loadDialog =  LoadDialog.getInstance();
        loadDialog.setStyle(DialogFragment.STYLE_NORMAL,R.style.load_dialog);
        LoadDialog.getInstance().show(getSupportFragmentManager(),"");
    }

    private void MyshowDialog(){
        MyLoadDialog myLoadDialog=MyLoadDialog.getInstance();
        myLoadDialog.setStyle(DialogFragment.STYLE_NORMAL,R.style.load_dialog);
        MyLoadDialog.getInstance().show(getSupportFragmentManager(),"");
    }

    private ArrayList<MarkerOptions> parseJSONWithJSONObject(String jsonData){//解析JSON数据并返回一个List<MarkerOptions>
        ArrayList<MarkerOptions> markerOptions=new ArrayList<MarkerOptions>();
        BitmapDescriptor bda= BitmapDescriptorFactory//素材a
                .fromResource(R.drawable.bda);
        BitmapDescriptor bdb=BitmapDescriptorFactory//素材b
                .fromResource(R.drawable.bdb);
        ArrayList<BitmapDescriptor> giflist=new ArrayList<BitmapDescriptor>();//将动态图素材加载进来
        giflist.add(bda);
        giflist.add(bdb);
        try{
            BitmapDescriptor icon= BitmapDescriptorFactory
                    .fromResource(R.drawable.mark);
            JSONArray jsonArray=new JSONArray(jsonData);
            for (int i=0;i<jsonArray.length();i++){
                JSONObject jsonObject=jsonArray.getJSONObject(i);
                String username=jsonObject.getString("username");
                String latitude=jsonObject.getString("latitude");
                String longtitude=jsonObject.getString("longtitude");

                Log.d("MainActivity","username is "+username);
                Log.d("MainActivity","latitude is "+latitude);
                Log.d("MainActivity","longtitude is "+longtitude);

                double lat=Double.parseDouble(latitude);
                double lng=Double.parseDouble(longtitude);
                LatLng point =new LatLng(lat,lng);
                MarkerOptions option=new MarkerOptions()
                        .position(point)
                        .icons(giflist)//icons表示多张图片合成的动态图gif，若是要静态图，将其改成icon,再换一张静态图png即可
                        .zIndex(0)
                        .period(40)
                        .draggable(false)//设置为可拖拽
                        .setFlat(false);//设置marker平贴地图效果
                markerOptions.add(option);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return markerOptions;
    }

    @Override
    public void onMyLocationChange(Location location) {//这个回调方法非常重要，对于控制回调时可控制加入事件
        mStartPoint=new LatLonPoint(location.getLatitude(),location.getLongitude());
        Mylocation();
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
               mEndPoint=new LatLonPoint(marker.getPosition().latitude,marker.getPosition().longitude);

        if (tempMarker!=null){/**说明傀儡Marker已经指向一个Marker**/
            tempMarker=null;/**将傀儡Marker指向null**/
            mWalkRouteOverlay.removeFromMap();/**去除上一点的涂层**/
        }
        //marker.showInfoWindow();
        //mWalkRouteOverlay.removeFromMap();
        new Thread(new Runnable() {
            @Override
            public void run() {
                searchRouteResult(ROUTE_TYPE_WALK,RouteSearch.WALK_DEFAULT);/**点击后，发起请求**/
                tempMarker=marker;/**傀儡Marker指向该Marker，即点击的那个Marker**/
            }
        }).start();

        return true;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        Log.d(TAG,"getInfoWindow");
        View infoWindow=getLayoutInflater().inflate(R.layout.info_window,null);
        render(marker,infoWindow);
        return infoWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        Log.d(TAG,"getInfoContents");
        return null;
    }
    /**自定义infowindow窗口**/
    public void render(Marker marker,View view){
        TextView tv_time=(TextView)view.findViewById(R.id.tv_time_min);
        TextView tv_time_info=(TextView)view.findViewById(R.id.tv_time_sec);
        TextView tv_distance=(TextView)view.findViewById(R.id.tv_distance);
        tv_time.setText(timeMin+"min");
        tv_time_info.setText(timeSec+"sec");
        tv_distance.setText(distance+"m");
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult result, int errorCode) {
        LoadDialog.getInstance().dismiss();
        if (errorCode==AMapException.CODE_AMAP_SUCCESS){
            if (result!=null&&result.getPaths()!=null){
                if (result.getPaths().size()>0){
                    mWalkRouteResult=result;
                    final WalkPath walkPath=mWalkRouteResult.getPaths().get(0);
                    mWalkRouteOverlay=new WalkRouteOverlay(this,aMap,walkPath,
                            mWalkRouteResult.getStartPos(),mWalkRouteResult.getTargetPos());
                    mWalkRouteOverlay.removeFromMap();
                    mWalkRouteOverlay.addToMap();
                    mWalkRouteOverlay.zoomToSpan();
                    int dis=(int)walkPath.getDistance();//获得起始点到终点的距离
                    int dur=(int)walkPath.getDuration();//获得地始点到终点的时间
                    timeMin=new Integer(dur/60).toString();
                    timeSec=new Integer(dur%60).toString();
                    distance=new Integer(dis).toString();
                    tempMarker.showInfoWindow();/**发起请求后，等待onWalkRouteSearched（）这个回调接口的
                                                    调用，此次调用成功后即说明数据接收和解析成功，再显示InfoWindow信息窗口**/
                    //Toast.makeText(this,"dis:"+dis+"|"+"dur:"+dur,Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"dis:"+dis+"|"+"dur:"+dur);
                }else if(result!=null&&result.getPaths()==null){
                    Toast.makeText(this,"对不起，没有搜索到相关数据！",Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this,"对不起，没有搜索到相关数据！",Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this,errorCode,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }
    /**
     * 开始搜索路径规划方案
     * **/
        public void searchRouteResult(int routeType,int mode){
            if (mStartPoint==null){
                Toast.makeText(this,"定位中，稍后再试...",Toast.LENGTH_SHORT).show();
                return;
            }
            if (mEndPoint==null){
                Toast.makeText(this,"终点未设置！",Toast.LENGTH_SHORT).show();
            }
            showDialog();
            final RouteSearch.FromAndTo fromAndTo =new RouteSearch.FromAndTo(mStartPoint,mEndPoint);
            if (routeType==ROUTE_TYPE_WALK){//步行路径规划
                RouteSearch.WalkRouteQuery query=new RouteSearch.WalkRouteQuery(fromAndTo);
                mRouteSearch.calculateWalkRouteAsyn(query);//异步路径步行模式查询
            }
        }
    /******************************class define*******************************************************/
    class GetMarker extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... strings) {
            String responseData = null;
            try{
                OkHttpClient client =new OkHttpClient();
                Request request=new Request.Builder()
                        .url("http://120.79.91.50/locationTojson.php")
                        .build();
                Response response=client.newCall(request).execute();
                if (response.code()==200){
                    responseData=response.body().string();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return responseData ;
        }

        @Override
        protected void onPostExecute(String responseData) {
            markerlocjsonData=responseData;
            if (markerlocjsonData!=null){
                isdone=true;
            }
        }
    }

}
