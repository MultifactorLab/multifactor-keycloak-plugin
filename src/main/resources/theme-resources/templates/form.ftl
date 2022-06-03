<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        ${msg("loginTitle",realm.name)}
    <#elseif section = "header">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        ${msg("loginTitleHtml",realm.name)}
    <#elseif section = "form">
        <script src="${url.resourcesPath}/KeyCloakScript.js" type="text/javascript"></script>
        <iframe id="mf_iframe" data-request-url="${request_url}" data-post-action="${url.loginAction}">
        </iframe>
        <style>     
            #kc-header {margin-top: -70px;}
            .card-pf {background-color: #eff3f6;
                      margin-top: -50px;}
            #mf_iframe {
                width: 430px;
                height: 680px;
                border: none;
            }
        </style>
    </#if>
</@layout.registrationLayout>
