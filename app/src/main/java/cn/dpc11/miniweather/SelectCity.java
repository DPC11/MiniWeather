package cn.dpc11.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.dpc11.app.MyApplication;
import cn.dpc11.bean.City;

/**
 * Created by DPC on 2016/10/11.
 */

public class SelectCity extends Activity implements View.OnClickListener {
    private ImageView mBackBtn;
    private TextView cityNameTv;
    private List<City> mCityList;

    private AdapterView.OnItemClickListener mMessageClickedHandler =
            new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            String cityCode = mCityList.get(position).getNumber();

            Intent i = new Intent();
            i.putExtra("cityCode", cityCode);
            setResult(RESULT_OK, i);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_city);
        cityNameTv = (TextView) findViewById(R.id.title_name);
        cityNameTv.setText("当前城市：" + this.getIntent().getStringExtra("cityName"));

        MyApplication myApplication = (MyApplication) getApplication();
        mCityList = myApplication.getmCityList();
        setCityListView();

        mBackBtn = (ImageView) findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }

    private void setCityListView() {
        ListView listView = (ListView) findViewById(R.id.city_list);

        List<String> cityList = new ArrayList<>();
        for (City city : mCityList) {
            cityList.add(city.toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, cityList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(mMessageClickedHandler);
    }
}
