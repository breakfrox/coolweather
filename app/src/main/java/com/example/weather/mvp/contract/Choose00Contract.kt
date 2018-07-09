package com.example.weather.mvp.contract

import com.example.weather.base.BasePresenter
import com.example.weather.base.BaseView
import com.example.weather.network.gson.HeHotCity

interface Choose00Contract{
    interface View: BaseView<Presenter>{
        abstract fun showHotCity(basic: List<HeHotCity.HeWeather6Bean.BasicBean>)
        fun showMessage(message: String)

    }

    interface Presenter: BasePresenter{
        fun getHotCity()
    }
}