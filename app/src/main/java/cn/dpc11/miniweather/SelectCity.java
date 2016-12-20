package cn.dpc11.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
    private EditText mEditText;

    private ListView mListView;
    private List<City> mCityList;
    private ArrayAdapter<City> mAdapter;

    private AdapterView.OnItemClickListener mMessageClickedHandler =
            new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            String cityCode = ((City) parent.getItemAtPosition(position)).getNumber();

            Intent i = new Intent();
            i.putExtra("cityCode", cityCode);
            setResult(RESULT_OK, i);
            finish();
        }
    };

    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String cs = mEditText.getText().toString().toUpperCase();
            mAdapter.getFilter().filter(cs);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    OnScrollListener scrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState != 0) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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

        mListView = (ListView) findViewById(R.id.city_list);
        mAdapter = new ArrayAdapter<>(this, R.layout.list_item, mCityList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mMessageClickedHandler);
        mListView.setOnScrollListener(scrollListener);

        mBackBtn = (ImageView) findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);

        mEditText = (EditText) findViewById(R.id.search_edit);
        mEditText.addTextChangedListener(mTextWatcher);
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
}
