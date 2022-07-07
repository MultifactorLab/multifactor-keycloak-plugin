
(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        var mf = factory();
        mf._onReady(mf.init);
        root.mf = mf;
  }
}(this, function() {

    var postAction,
        postArgument,
        arUrl,
        iframe,
        submitCallback;
    initializeStatefulVariables();

    /**
     * Set local variables to whatever they should be before you call init().
     */
    function initializeStatefulVariables() {
        postAction = '';
        postArgument = 'jwt_token';
        arUrl = undefined;
        iframe = undefined;
        submitCallback = undefined;
    }

    function throwError(message) {
        throw new Error(
            'Error: ' + message
        );
    }

    function hyphenize(str) {
        return str.replace(/([a-z])([A-Z])/, '$1-$2').toLowerCase();
    }

    // cross-browser data attributes
    function getDataAttribute(element, name) {
        if ('dataset' in element) {
            return element.dataset[name];
        } else {
            return element.getAttribute('data-' + hyphenize(name));
        }
    }

    // cross-browser event binding/unbinding
    function on(context, event, fallbackEvent, callback) {
        if ('addEventListener' in window) {
            context.addEventListener(event, callback, false);
        } else {
            context.attachEvent(fallbackEvent, callback);
        }
    }

    function off(context, event, fallbackEvent, callback) {
        if ('removeEventListener' in window) {
            context.removeEventListener(event, callback, false);
        } else {
            context.detachEvent(fallbackEvent, callback);
        }
    }

    function onReady(callback) {
        on(document, 'DOMContentLoaded', 'onreadystatechange', callback);
    }

    function offReady(callback) {
        off(document, 'DOMContentLoaded', 'onreadystatechange', callback);
    }

    function onMessage(callback) {
        on(window, 'message', 'onmessage', callback);
    }

    function offMessage(callback) {
        off(window, 'message', 'onmessage', callback);
    }


    /**
     * Prepare for the iframe to become ready.
     *
     */
    function init(options) {
        
        initializeStatefulVariables();
        var promptElement = getPromptElement(options);

        if (promptElement) {
            // If we can get the element that will host the prompt, set it.
            ready(promptElement, options.iframeAttributes || {});
        } else {
            // If the element that will host the prompt isn't available yet, set
            // it up after the DOM finishes loading.
            asyncReady(options);
        }

        // always clean up after yourself!
        offReady(init);
    }

    /**
     * Given the options from init(), get the iframe.
     */
    function getPromptElement(options) {
        var result;
        result = document.getElementById('mf_iframe');
        return result;
    }


    /**
     * Check if the given thing is an iframe.
     */
    function isIframe(element) {
        return (
            element &&
            element.tagName &&
            element.tagName.toLowerCase() === 'iframe'
        );
    }


    /**
     * This function is called when a message was received.
     */
    function onReceivedMessage(event) {
          //make sure message from known origin
          if (arUrl.indexOf(event.origin, 0)===0){
            doPostBack(event.data);
            // always clean up after yourself!
            offMessage(onReceivedMessage);
          }   
    }

    /**
     * Register a callback to call ready() after the DOM has loaded.
     */
    function asyncReady(options) {
        var callback = function() {
            var promptElement = getPromptElement(options);
            if (!promptElement) {
                throwError(
                    'This page does not contain an iframe for use.' +
                    ' Add an element like' +
                    ' <iframe id="mf_iframe"></iframe> to this page.'
                );
            }

            ready(promptElement, options.iframeAttributes || {});

            // Always clean up after yourself.
            offReady(callback)
        };

        onReady(callback);
    }

    /**
     * Point the iframe at Multifactor, then wait for it to postMessage back to us.
     */
    function ready(promptElement, iframeAttributes) {
        if (!arUrl) {
            arUrl = getDataAttribute(promptElement, 'requestUrl');
            if (!arUrl) {
                throwError(
                    'No acces request URL is given for use.  Be sure to set ' +
                    '`data-request-url` attribute on the iframe element.'
                );
            }
        }
        
        if (postAction === '') {
            postAction = getDataAttribute(promptElement, 'postAction') || postAction;
        }


        iframe = promptElement;
        iframe.src = arUrl;

        // listen for the 'message' event
        onMessage(onReceivedMessage);
    }

    /**
     * We received a postMessage from Multifactor.  POST back to the primary service
     * with the response token.
     */
    function doPostBack(response) {
        // create a hidden input to contain the response token
        var input = document.createElement('input');
        input.type = 'hidden';
        input.name = postArgument;
        input.value = response;

        var form = document.createElement('form')
        iframe.parentElement.insertBefore(form, iframe.nextSibling);

        // make sure we are actually posting to the right place
        form.method = 'POST';
        form.action = postAction;

        // add the response token input to the form
        form.appendChild(input);

        // away we go!
        if (typeof submitCallback === "function") {
            submitCallback.call(null, form);
        } else {
            form.submit();
        }
    }

    return {
        init: init,
        _onReady: onReady,
        _doPostBack: doPostBack
    };
}));
