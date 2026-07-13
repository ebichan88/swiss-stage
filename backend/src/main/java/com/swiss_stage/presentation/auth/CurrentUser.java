package com.swiss_stage.presentation.auth;

/** 認証済み運営者(JWTクレームから復元)。request attributeで受け渡す */
public record CurrentUser(String sub, String name) {

    public static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();
}
