package com.hcl.kandy.cpass.remote;


import com.hcl.kandy.cpass.remote.models.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RestApiInterface {
    @FormUrlEncoded
    @POST(API.LOGIN_URL)
    Call<LoginResponse> loginAPI(@Field("username") String username,
                                 @Field("password") String password,
                                 @Field("client_id") String client_id,
                                 @Field("grant_type") String grant_type,
                                 @Field("scope") String scope);

    @FormUrlEncoded
    @POST(API.LOGIN_URL)
    Call<LoginResponse> loginAPIProject(@Field("client_id") String client_id,
                                        @Field("client_secret") String client_secret,
                                        @Field("grant_type") String grant_type,
                                        @Field("scope") String scope);
}
