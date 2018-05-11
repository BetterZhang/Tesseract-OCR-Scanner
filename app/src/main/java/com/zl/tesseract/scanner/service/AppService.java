package com.zl.tesseract.scanner.service;

import com.zl.tesseract.scanner.domain.ResponseBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Android Studio.
 * Author : zhangzhongqiang
 * Email  : betterzhang.dev@gmail.com
 * Time   : 2018/05/09 下午 3:44
 * Desc   : description
 */
public interface AppService {

    @POST("/icr/recognize_document")
    Call<ResponseBody> scanner(@Query("user") String user, @Query("password") String password, @Body RequestBody photo);

}
