package cn.dpc11.miniweather;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.dpc11.adapter.ViewPagerAdapter;
import cn.dpc11.bean.FutureWeather;
import cn.dpc11.bean.TodayWeather;
import cn.dpc11.service.UpdateService;
import cn.dpc11.util.NetUtil;

/**
 * Created by DPC on 16/9/20.
 */
public class MainActivity extends Activity implements View.OnClickListener, ViewPager.OnPageChangeListener{
    private static final int UPDATE_TODAY_WEATHER = 1;
    // HashMap 一一对应天气类型与天气图片资源
    private static final HashMap<String, Integer> weatherRes = new HashMap<>();

    static {
        weatherRes.put("晴", R.drawable.biz_plugin_weather_qing);
        weatherRes.put("暴雪", R.drawable.biz_plugin_weather_baoxue);
        weatherRes.put("暴雨", R.drawable.biz_plugin_weather_baoyu);
        weatherRes.put("大暴雨", R.drawable.biz_plugin_weather_dabaoyu);
        weatherRes.put("大雪", R.drawable.biz_plugin_weather_daxue);
        weatherRes.put("大雨", R.drawable.biz_plugin_weather_dayu);
        weatherRes.put("多云", R.drawable.biz_plugin_weather_duoyun);
        weatherRes.put("雷阵雨", R.drawable.biz_plugin_weather_leizhenyu);
        weatherRes.put("雷阵雨冰雹", R.drawable.biz_plugin_weather_leizhenyubingbao);
        weatherRes.put("沙尘暴", R.drawable.biz_plugin_weather_shachenbao);
        weatherRes.put("特大暴雨", R.drawable.biz_plugin_weather_tedabaoyu);
        weatherRes.put("雾", R.drawable.biz_plugin_weather_wu);
        weatherRes.put("小雪", R.drawable.biz_plugin_weather_xiaoxue);
        weatherRes.put("小雨", R.drawable.biz_plugin_weather_xiaoyu);
        weatherRes.put("阴", R.drawable.biz_plugin_weather_yin);
        weatherRes.put("雨夹雪", R.drawable.biz_plugin_weather_yujiaxue);
        weatherRes.put("阵雪", R.drawable.biz_plugin_weather_zhenxue);
        weatherRes.put("阵雨", R.drawable.biz_plugin_weather_zhenyu);
        weatherRes.put("中雪", R.drawable.biz_plugin_weather_zhongxue);
        weatherRes.put("中雨", R.drawable.biz_plugin_weather_zhongyu);
    }

    private ImageView mUpdateBtn;
    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, cityNameTv, currentTempTv;
    private ImageView weatherImg, pmImg;
    private ImageView mCitySelect;
    private String currentCityCode;
    private TodayWeather currentWeather;

    private IntentFilter intentFilter;

    private ViewPagerAdapter vpAdapter;
    private ViewPager vp;
    private List<View> views;
    private List<FutureWeather> futureWeathers;

    private ImageView[] dots;
    private int[] ids = {R.id.iv1, R.id.iv2};

    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetUtil.getNetworkState(getBaseContext()) != NetUtil.NetworkState.NETWORK_NONE) {
                Log.d("MyWeather", "网络OK");
                queryWeatherCode(currentCityCode);
            } else {
                Log.d("MyWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    };

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_LONG).show();

                    mUpdateBtn.setAnimation(null);
                    mUpdateBtn.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);

        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        initView();

        // 判断是否是新建 Activity 还是从之前状态恢复
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        if (savedInstanceState != null) {
            currentWeather = (TodayWeather) savedInstanceState.getSerializable("currentWeather");
            currentCityCode = sharedPreferences.getString("current_city", "101010100");
            updateTodayWeather(currentWeather);
        } else {
            currentCityCode = sharedPreferences.getString("current_city", "101010100");
            Log.d("MyWeather", currentCityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NetworkState.NETWORK_NONE) {
                Log.d("MyWeather", "网络OK");
                queryWeatherCode(currentCityCode);
            } else {
                Log.d("MyWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }

        // 启动自动刷新 Service，并接受自动刷新的广播通知
        startService(new Intent(getBaseContext(), UpdateService.class));
        intentFilter = new IntentFilter();
        intentFilter.addAction("Update");
        registerReceiver(intentReceiver, intentFilter);
    }

    void initView() {
        cityNameTv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        currentTempTv = (TextView) findViewById(R.id.current_temp);
        cityNameTv.setText("N/A");
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        weekTv.setText("N/A");
        temperatureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        currentTempTv.setText("N/A");

        // 初始化底部 ViewPager
        LayoutInflater inflater = LayoutInflater.from(this);
        views = new ArrayList<>();
        futureWeathers = new ArrayList<>();
        views.add(inflater.inflate(R.layout.future_weather, null));
        views.add(inflater.inflate(R.layout.future_weather, null));
        vpAdapter = new ViewPagerAdapter(views, this);
        vp = (ViewPager) findViewById(R.id.view_pager);
        vp.setAdapter(vpAdapter);
        vp.addOnPageChangeListener(this);

        for (int i = 0; i < views.size(); i++) {
            LinearLayout view = (LinearLayout) views.get(i);
            for (int k = 0; k < view.getChildCount(); k++) {
                View subview = view.getChildAt(k);
                for (int j = 0; j < 3; j++) {
                    ((TextView) subview.findViewById(R.id.tomorrow_date)).setText("N/A");
                    ((TextView) subview.findViewById(R.id.tomorrow_temp)).setText("N/A");
                    ((TextView) subview.findViewById(R.id.tomorrow_weather)).setText("N/A");
                    ((TextView) subview.findViewById(R.id.tomorrow_wind)).setText("N/A");
                }
            }
        }

        // 初始化底部小圆点
        dots = new ImageView[views.size()];
        for (int i = 0; i < views.size(); i++) {
            dots[i] = (ImageView) findViewById(ids[i]);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            // 响应选择城市按钮点击
            case R.id.title_city_manager:
                Intent i = new Intent(this, SelectCity.class);
                i.putExtra("cityName", cityNameTv.getText());
                startActivityForResult(i, 1);
                break;

            // 响应刷新按钮点击
            case R.id.title_update_btn:
                RotateAnimation rotate = new RotateAnimation(0.0f, 360.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setInterpolator(new LinearInterpolator());
                rotate.setRepeatCount(Animation.INFINITE);
                rotate.setDuration(400);

                mUpdateBtn.setEnabled(false);
                mUpdateBtn.startAnimation(rotate);

                if (NetUtil.getNetworkState(this) != NetUtil.NetworkState.NETWORK_NONE) {
                    Log.d("MyWeather", "网络OK");
                    queryWeatherCode(currentCityCode);
                } else {
                    Log.d("MyWeather", "网络挂了");
                    Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    private void queryWeatherCode(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("MyWeather", address);

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);

                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        response.append(str);
                    }
                    String responseStr = response.toString();
                    Log.d("MyWeather", responseStr);
                    todayWeather = parseXML(responseStr);
                    if (todayWeather != null) {
                        currentWeather = todayWeather;
                        Log.d("MyWeather", todayWeather.toString());

                        // 只能在主线程中刷新 UI 控件，所以需要消息机制
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;
        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;

        futureWeathers.clear();
        FutureWeather futureWeather = null;
        int startFurture = 0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));

            int eventType = xmlPullParser.getEventType();
            Log.d("MyWeather", "parseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {

                    // 判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    // 判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("city")) {
                            xmlPullParser.next();
                            todayWeather = new TodayWeather();
                            todayWeather.setCity(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("updatetime")) {
                            xmlPullParser.next();
                            todayWeather.setUpdatetime(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("shidu")) {
                            xmlPullParser.next();
                            todayWeather.setShidu(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("wendu")) {
                            xmlPullParser.next();
                            todayWeather.setWendu(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("pm25")) {
                            xmlPullParser.next();
                            todayWeather.setPm25(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("quality")) {
                            xmlPullParser.next();
                            todayWeather.setQuality(xmlPullParser.getText());

                        } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setFengxiang(xmlPullParser.getText());
                            fengxiangCount++;

                        } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setFengli(xmlPullParser.getText());
                            fengliCount++;

                        } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setDate(xmlPullParser.getText());
                            dateCount++;

                            futureWeather = new FutureWeather();
                            startFurture = 1;
                            futureWeather.setDate(xmlPullParser.getText());
                        } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                            xmlPullParser.next();

                            // 分解出温度数字部分, "高温 20℃" -> "20℃"
                            todayWeather.setHigh(xmlPullParser.getText().split(" ")[1]);
                            futureWeather.setHigh(xmlPullParser.getText().split(" ")[1]);
                            highCount++;

                        } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setLow(xmlPullParser.getText().split(" ")[1]);
                            futureWeather.setLow(xmlPullParser.getText().split(" ")[1]);
                            lowCount++;

                        } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                            xmlPullParser.next();
                            todayWeather.setType(xmlPullParser.getText());
                            futureWeather.setType(xmlPullParser.getText());
                            typeCount++;

                        } else if ((xmlPullParser.getName().equals("date") || xmlPullParser.getName().equals("date_1")) && startFurture == 0) {
                            futureWeather = new FutureWeather();
                            startFurture = 1;

                            xmlPullParser.next();
                            futureWeather.setDate(xmlPullParser.getText());
                        } else if ((xmlPullParser.getName().equals("high") || xmlPullParser.getName().equals("high_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setHigh(xmlPullParser.getText().split(" ")[1]);

                        } else if ((xmlPullParser.getName().equals("low") || xmlPullParser.getName().equals("low_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setLow(xmlPullParser.getText().split(" ")[1]);

                        } else if ((xmlPullParser.getName().equals("type") || xmlPullParser.getName().equals("type_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setType(xmlPullParser.getText());

                        } else if ((xmlPullParser.getName().equals("fengxiang") || xmlPullParser.getName().equals("fx_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setFengxiang(xmlPullParser.getText());

                        } else if ((xmlPullParser.getName().equals("fengli") || xmlPullParser.getName().equals("fl_1")) && startFurture == 1) {
                            xmlPullParser.next();
                            futureWeather.setFengli(xmlPullParser.getText());

                            startFurture = 0;
                            if (futureWeather.getDate() != null)
                                futureWeathers.add(futureWeather);
                        }
                        break;
                    // 判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                // 进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    void updateTodayWeather(TodayWeather todayWeather) {
        cityNameTv.setText(todayWeather.getCity() + "天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime() + "发布");
        humidityTv.setText("湿度：" + todayWeather.getShidu());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getLow() + "~" + todayWeather.getHigh());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力：" + todayWeather.getFengli());
        currentTempTv.setText(todayWeather.getWendu() + "℃");
        if (weatherRes.get(todayWeather.getType()) != null) {
            weatherImg.setImageResource(weatherRes.get(todayWeather.getType()));
        }

        if (todayWeather.getPm25() != null) {
            pmDataTv.setText(todayWeather.getPm25());
            pmQualityTv.setText(todayWeather.getQuality());

            int pm25 = Integer.parseInt(todayWeather.getPm25());
            if (pm25 <= 50) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
            } else if (pm25 <= 100) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
            } else if (pm25 <= 150) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
            } else if (pm25 <= 200) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
            } else if (pm25 <= 300) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
            } else {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
            }
        } else {
            pmDataTv.setText("N/A");
            pmQualityTv.setText("N/A");
        }

        // 顺便更新六日天气
        for (int i = 0; i < views.size(); i++) {
            LinearLayout view = (LinearLayout) views.get(i);
            for (int j = 0; j < view.getChildCount(); j++) {
                View subview = view.getChildAt(j);
                FutureWeather futureWeather = futureWeathers.get(i * view.getChildCount() + j);

                ((TextView) subview.findViewById(R.id.tomorrow_date)).setText(futureWeather.getDate());
                ((ImageView) subview.findViewById(R.id.weather_img)).setImageResource(weatherRes.get(todayWeather.getType()));
                ((TextView) subview.findViewById(R.id.tomorrow_temp)).setText(futureWeather.getLow() + "~" + futureWeather.getHigh());
                ((TextView) subview.findViewById(R.id.tomorrow_weather)).setText(futureWeather.getType());
                ((TextView) subview.findViewById(R.id.tomorrow_wind)).setText(futureWeather.getFengli());

            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            currentCityCode = data.getStringExtra("cityCode");
            Log.d("MyWeather", "选择的城市代码为" + currentCityCode);

            // 在配置文件中更新城市
            SharedPreferences.Editor editor = getSharedPreferences("config", MODE_PRIVATE).edit();
            editor.putString("current_city", currentCityCode);
            editor.commit();

            if (NetUtil.getNetworkState(this) != NetUtil.NetworkState.NETWORK_NONE) {
                Log.d("MyWeather", "网络OK");
                queryWeatherCode(currentCityCode);
                Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_LONG).show();
            } else {
                Log.d("MyWeather", "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // 在 Activity 可能被摧毁之前保存目前状态
        outState.putSerializable("currentWeather", currentWeather);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(new Intent(getBaseContext(), UpdateService.class));
        unregisterReceiver(intentReceiver);
    }
    

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        for (int i = 0; i< ids.length; i++) {
            if (i == position) {
                dots[i].setImageResource(R.drawable.page_indicator_focused);
            } else {
                dots[i].setImageResource(R.drawable.page_indicator_unfocused);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
