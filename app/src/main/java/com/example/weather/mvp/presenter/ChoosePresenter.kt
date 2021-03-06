package com.example.weather.mvp.presenter

import android.app.Activity
import com.example.weather.mvp.contract.ChooseContract
import com.example.weather.other.db.City
import com.example.weather.other.db.County
import com.example.weather.other.db.Province
import com.example.weather.ui.choose.ChooseFragment
import com.example.weather.util.HttpUtil
import com.example.weather.util.Utility
import okhttp3.Call
import okhttp3.Response
import org.litepal.crud.DataSupport
import java.io.IOException

/**
 * bug:　网络访问，有几个地点的天气是无法获取的，这是api的问题
 */
class ChoosePresenter(val mView: ChooseContract.View,
                      val activity: Activity)
    : ChooseContract.Presenter {

    init {
        mView.presenter = this
    }


    private lateinit var provinceList: List<Province>
    private lateinit var cityList: List<City>
    override lateinit var countyList: List<County>

    private lateinit var selectedProvince: Province
    private lateinit var selectedCity: City

    private val dataList = arrayListOf<String>()
    private val PROVINCE = "province"
    private val CITY = "city"
    private val COUNTY = "county"
    /**
     * 优先查询数据库,其次网络
     */
    override fun queryProvinces() {
        mView.setupToolbar("中国", true)
        provinceList = DataSupport.findAll(Province::class.java)
        if (provinceList.size > 0) {
            dataList.clear()
            provinceList.forEach { dataList.add(it.name) }
            mView.showChange(dataList, ChooseFragment.LEVEL_PROVINCE)
        } else {
            val url = "http://guolin.tech/api/china/"
            queryFromService(url, PROVINCE)
        }
    }

    /**
     * 查询市级
     */

    override fun queryCities(position: Int) {
        if (position != -1) selectedProvince = provinceList.get(position)
        mView.setupToolbar(selectedProvince.name, true)
        cityList = DataSupport.where("provinceid=?", "${selectedProvince.id}")
                .find(City::class.java)
        if (cityList.size > 0) {
            dataList.clear()
            cityList.forEach { dataList.add(it.cityName) }
            mView.showChange(dataList, ChooseFragment.LEVEL_CITY)
        } else {
            val url = "http://guolin.tech/api/china/${selectedProvince.code}"
            queryFromService(url, CITY)
        }
    }

    /**
     * 查询县级数据
     */
    override fun queryCounties(position: Int) {
        if (position != -1) selectedCity = cityList[position]
        mView.setupToolbar(selectedCity.cityName, true)
        countyList = DataSupport.where("cityid=?", "${selectedCity.id}")
                .find(County::class.java)
        if (countyList.size > 0) {
            dataList.clear()
            countyList.forEach { dataList.add(it.countyName) }
            mView.showChange(dataList, ChooseFragment.LEVEL_COUNTY)
        } else {
            val url = "http://guolin.tech/api/china/${selectedProvince.code}/${selectedCity.cityCode}"
            queryFromService(url, COUNTY)
        }
    }

    private fun queryFromService(url: String, type: String) {
        mView.showProgress()
        HttpUtil.sendOkHttpRequest(url, object : okhttp3.Callback {
            override fun onResponse(call: Call?, response: Response) {
                val responseText = response.body()!!.string()
                val result = when (type) {
                    PROVINCE -> Utility.handlerProvince(responseText)
                    CITY -> Utility.handleCityResponse(responseText, selectedProvince.id)
                    COUNTY -> Utility.handleCountyResponse(responseText, selectedCity.id)
                    else -> false
                }
                if (result) {
                    activity.runOnUiThread {
                        mView.closeProgress()
                        when (type) {
                            PROVINCE -> queryProvinces()
                            CITY -> queryCities()
                            COUNTY -> queryCounties()
                        }
                    }
                }
            }

            override fun onFailure(call: Call?, e: IOException?) {
                mView.closeProgress()
                mView.showMessage("加载失败")
            }
        })
    }
}