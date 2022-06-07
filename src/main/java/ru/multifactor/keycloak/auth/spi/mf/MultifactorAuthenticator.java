package ru.multifactor.keycloak.auth.spi.mf;

import static io.restassured.RestAssured.get;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import io.restassured.http.ContentType;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.*;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//import java.util.Base64;
import java.util.Random;
import java.io.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.commons.codec.binary.StringUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import java.util.Date;

import static ru.multifactor.keycloak.auth.spi.mf.MultifactorAuthenticatorFactory.*;

public class MultifactorAuthenticator implements Authenticator{

    public MultifactorAuthenticator() {}

    @Override
    public boolean requiresUser() {
        // No user-specific configuration needed
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // No user-specific configuration needed, therefore always "configured"
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    public static boolean chkToken(String token, String user, String apiKey, String secret, StringBuilder result)
    {
        String[] parts=token.split("\\.");
        if(parts.length!=3)
	{
           result.append("ERR: Invalid token");
           return false;
        }
        String head = parts[0];
        String body = parts[1];
        String sign = parts[2];

        byte[] key = StringUtils.getBytesUtf8(secret);
        byte[] message = StringUtils.getBytesUtf8(head+"."+body);
        HmacUtils utils=new HmacUtils("HmacSHA256",key);
        String hash = Base64.encodeBase64URLSafeString(utils.hmac(message));
        if(!sign.equals(hash))
	{
           result.append("ERR: Invalid token signature");
           return false;
        }
        String decodedBody=StringUtils.newStringUtf8(Base64.decodeBase64(body));
        JSONObject res = new JSONObject(decodedBody);
        if(!res.has("aud") || !res.getString("aud").equals(apiKey))
	{
           result.append("ERR: Invalid token audience");
           return false;
        }
        Date now= new Date();
        if(!res.has("exp")||now.getTime()>res.getLong("exp")*1000)
	{
           result.append("ERR: Expired token");
           return false;
        }
        if(!res.has("orig_user") || !res.getString("orig_user").equals(user))
	{
           result.append("ERR: Wrong user name");
           return false;
        }
        return true;
	

    }
    public static boolean apiRequest(String url, String user, String apiKey, String secret, StringBuilder result)
    {
      JSONObject requestBody = new JSONObject();
      JSONObject requestCallback = new JSONObject();
      JSONObject claims = new JSONObject();
      claims.put("orig_user", user);
      requestCallback.put("action","javascript:window.parent.postMessage($`AccessToken`,'*')");
      requestCallback.put("target","_self");
      requestBody.put("identity", user);
      requestBody.put("claims", claims);
      requestBody.put("callback", requestCallback);
      JSONObject respObject;
      io.restassured.response.Response response;
      int statusCode; 
      try
      {
      	response= RestAssured.given().relaxedHTTPSValidation().auth().preemptive().basic(apiKey,secret)
                        .contentType(ContentType.JSON).body(StringUtils.getBytesUtf8(requestBody.toString())).post(url)
                        .then().extract().response();
      	statusCode = response.getStatusCode();
      }
      catch(Exception e)
      {
        result.append("ERR: "+e.getMessage());
        return false;

      }
      try
      {
      	respObject=new JSONObject(response.asString());
      }
      catch(Exception e)
      {
        result.append("ERR: Wrong api answer");
        return false;

      }

      if(statusCode==200)
      {
        if(respObject.getBoolean("success"))
	{
           JSONObject modelObject=respObject.getJSONObject("model");
	   result.append(modelObject.getString("url"));
           return true;
	}
        result.append("ERR: "+respObject.getString("message"));
        return false;
      }
      if(respObject.has("title")) result.append("ERR: "+respObject.getString("title"));
      else result.append("ERR: unknown error");
      return false; 		
    }

    private javax.ws.rs.core.Response createMultifactorForm(AuthenticationFlowContext context, String error) {
        StringBuilder result=new StringBuilder("");
        LoginFormsProvider form;
        if(apiRequest(apiURL(context)+"/access/requests", context.getUser().getUsername(), apiKey(context), apiSecret(context), result))
          form=context.form().setAttribute("request_url",result.toString());
        else
        {
          form=context.form().setAttribute("request_url","");
          if (error == null) error=result.toString();
        }
        if (error != null) 
            form.setError(error);
        return form.createForm("form.ftl");
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
          context.challenge(createMultifactorForm(context, null));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.resetFlow();
            return;
        }
        if (!formData.containsKey("jwt_token")) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, createMultifactorForm(context, "missing token"));
            return;
        }
        String token= formData.getFirst("jwt_token");
        StringBuilder result=new StringBuilder("");
        if(!chkToken(token, context.getUser().getUsername(), apiKey(context), apiSecret(context), result))
	{
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, createMultifactorForm(context, result.toString()));
            return;
        }
        context.success();
    }

    @Override
    public void close() {}

    private String apiKey(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(PROP_KEY));
    }
    private String apiSecret(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(PROP_SECRET));
    }
    private String apiURL(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null) return "";
        return String.valueOf(config.getConfig().get(PROP_APIURL));
    }

}
