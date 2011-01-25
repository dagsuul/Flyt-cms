/*
 * Copyright 2009 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This script expects the following properties to be set:
 * * title
 * * contextPath
 * * loadingText
 *
 */

var stateHandler;

$(document).ready(function() {
    stateHandler = new openaksess.admin.StateHandler();
    openaksess.common.debug("common.$(document).ready()");
    if (window.dwr) {
        dwr.engine.setHeaders({ "X-Requested-With":"XMLHttpRequest"});
    }
    openaksess.admin.setWindowSize();
    openaksess.admin.ajaxSetup();
    //openaksess.admin.widgetmanager.init({context: '#Content'});
    openaksess.admin.bindGlobalButtons();
    openaksess.admin.createWidgets();
});


/********************************************************************************
 * Common functions for the OpenAksess admin interface.
 ********************************************************************************/

openaksess.admin = {

    bindGlobalButtons : function() {
        $("#OpenAksessInfoButton").click(function() {
            var selected = $("#TopicTabs").tabs('option', 'selected');
            var container = $("#TopicTabs .ui-tabs-panel").eq(selected);
            var topicMapId = $(".topicMapId", container).val();

            openaksess.common.modalWindow.open({title:properties.title, width: 660, height:550, iframe: true,
            href: properties.contextPath + '/OpenAksessInformation.action'});
        });
    },

    /**
     * Default implementation that can be overridden by the layout.
     *
     * @param elementProperties
     */
    setLayoutSpecificSizes : function (elementProperties){},
    


    /**
     * Adjusts the height and width of the iframe onload and onresize.
     */
    setWindowSize : function () {

        var doResize = false;

        $(window).bind('resize load', function(e) {
            openaksess.common.debug("openaksess.admin.setWindowSize(): " + e.type + " event received. Do resize? " + doResize);
            doResize = true;
        });

        /**
         * The window resize process could potentially be a resource consuming process.
         * It is therefore not appropriate to preform this operation at every fired resize event.
         * The interval between each resize is set to minimum 100ms.
         */
        var minResizeInteval = 100;

        setInterval(function(){
            if (doResize || bodyLargerThanWindow()) {
                doResize = false;

                var windowHeight = $(window).height();
                var windowWidth = $(window).width();
                var topHeight = $("#Top").height();
                var navigationWidth = $("#Navigation").width();
                var framesplitWidth = $("#Framesplit").outerWidth(true);

                openaksess.common.debug("openaksess.admin.setWindowSize(): windowHeight: " + windowHeight + ", windowWidth: " + windowWidth + ", topHeight: " + topHeight+", navigationWidth: "+navigationWidth+", framesplitWidth: "+framesplitWidth);

                $('#Content').css('height', (windowHeight-topHeight) + 'px');

                if (typeof openaksess.admin.setLayoutSpecificSizes == 'function') {
                    openaksess.common.debug("openaksess.openaksess.admin.setWindowSize(): setLayoutSpecificSizes function found");
                    openaksess.admin.setLayoutSpecificSizes({window:{height: windowHeight, width: windowWidth}, top:{height:topHeight}, navigation:{width:navigationWidth},framesplit:{width:framesplitWidth}});
                }

                $(document).ready(function() {
                    $("body").addClass("fuckIE7").removeClass("fuckIE7");
                });
            }
        }, minResizeInteval);

        /**
         * Under ideal conditions, the body should be as high as the view port (window).
         * Sometimes, for example when changing font size, the resize calculations fail, often beacause of a
         * temporary scroll bar added during the font size change process. This will cause the content frame iFrame
         * to wrap below the view port.
         *
         * To check if the content frame has wrapped below the window we check if the total body height is
         * higher than the view port.
         */
        function bodyLargerThanWindow() {
            //Add 100px to the calculation just to make sure that it's acutually a frame wrap situation that
            //has occurred, not just an odd pixel difference.
            return $(document).height() > $(window).height()+100;
        }
    },



    ajaxSetup :function () {
        var $ajaxloading = $('<div id="AjaxLoadingIllustration" class="ajaxloading">' + properties.loadingText + '</div>');
        $("body").append($ajaxloading);
        $ajaxloading.ajaxStart(function(){
            $(this).show();
        });

        $ajaxloading.ajaxStop(function(){
            $(this).hide();
        });

        $.ajaxSetup ({
            cache: false,
            error: function(xhr, textStatus, errorThrown) {
                if (xhr.status == 401) {
                    window.location.href = properties.contextPath + '/admin/?dummy=' + new Date().getTime();
                }
            }
        });        
    },

    /**
     * Sets the currently viewed content in the user's session.
     *
     * @param url
     */
    notifyContentUpdate : function(url) {
        ContentStateHandler.notifyContentUpdate(url);
    },


    /**
     * Handles browser history when using ajax page loading.
     *
     * See http://benalman.com/projects/jquery-bbq-plugin/ for further details about the bbq-plugin.
     */
    StateHandler : function (){

        var currentState = '';

        this.init = function(updateEventType) {
            openaksess.common.debug("common.StateHandler.init()");
            bindHashChange(updateEventType);
            $(window).trigger("hashchange");
        };

        this.setState = function(state) {
            openaksess.common.debug("common.StateHandler.setState(): state: " + state);
            $.bbq.pushState({ state : state });
        };

        this.getState = function() {
            return $.bbq.getState("state");
        };

        var bindHashChange = function(updateEventType) {
            openaksess.common.debug("common.StateHandler._bindHashChange(): Binding hashchange event");
            $(window).bind( 'hashchange', function(e) {
                //Get the url from the state
                var state = $.bbq.getState('state');
                openaksess.common.debug("common.StateHandler._bindHashChange(): hashchange event received. Url: " + state);

                if (state && state != this.currentState) {
                    openaksess.common.debug("common.StateHandler._bindHashChange(): New state ("+state+") different from current state ("+this.currentState+"). Triggering '"+updateEventType+ "' event.");
                    $.event.trigger(updateEventType, state);
                    currentState = state;
                }
            });
        };

    },


    /**
     * Handles user settings (perferences). These can either be per session or permanent (persistent).
     */
    userpreferences : {

        cookiePrefix : "userpreferences_",

        keys : {
            filter : {
                hideExpired: 'filterHideExpired',
                sort: 'filterSort',
                sites: 'filterSites'
            },
            multimedia : {
                currentfolder: 'currentMultimediaFolder',
                navigationwidth : 'multimediaNavigationWidth'
            },
            content : {
                navigationwidth : 'contentNavigationWidth'
            },
            formadmin : {
                navigationwidth : 'formadminNavigationWidth'
            }
        },

        /**
         * Saves a user preference.
         * @param key - one of the userpreferences.keys
         * @param value
         * @param permanent - If false, the preference is erased when the user session ends.
         * For client side stored preferences, a permanent preference is saved for one year.
         * @param callback - If supplied, the preference is saved server side.
         * Permanent server side preferences are stored in the databse, non-permanent ones in the session.
         * If not supplied, the preference is saved client side.
         */
        setPreference: function(key, value, permanent, callback){

            if (callback) {
                    UserPreferencesHandler.setPreference({key:key, value:value, permanent:permanent}, callback);
            } else {
                var ttl = undefined;
                if (permanent) {
                    ttl = 365;
                }
                openaksess.common.cookies.create(openaksess.admin.userpreferences.cookiePrefix+key, value, ttl);
            }
        },

        /**
         * Returns the value of a preference with the given key.
         * If callback is supplied, the preference is retrieved from server side storage.
         * @param key
         * @param callback
         */
        getPreference: function(key, callback) {
            if (callback) {
                UserPreferencesHandler.getPreference(key, callback);
            } else {
                return openaksess.common.cookies.read(openaksess.admin.userpreferences.cookiePrefix+key);
            }
        },

        deletePreference : function(key, callback) {
            if (callback) {
                UserPreferencesHandler.deletePreference(key, callback);
            } else {
                openaksess.common.cookies.erase(openaksess.admin.userpreferences.cookiePrefix+key);
            }
        }
    },


    widgetmanager : function() {

        var config = {
            context: 'body',
            widgetContainerClass: 'widgetcolumn',
            widgetClass: 'widget',
            widgetHeaderClass: 'widget-header',
            widgetContentClass: 'widget-content',
            minimizeClass: 'ui-icon ui-icon-minus',
            maximizeClass: 'ui-icon ui-icon-plus',
            closeClass: 'ui-icon ui-icon-close'
        };


        var _getElementAsJquery = function(elem) {
            if (elem instanceof jQuery) {
                return elem;
            } else {
                return $(elem);
            }
        };

        var minimize = function(widget) {
            var $widget = _getElementAsJquery(widget);
            $widget.find('.'+config.widgetHeaderClass+' .widget-controls :last').removeClass(config.minimizeClass).addClass(config.maximizeClass);
            $widget.find('.'+config.widgetContentClass).hide();
        };

        var maximize = function(widget) {
            var $widget = _getElementAsJquery(widget);
            $widget.find('.'+config.widgetHeaderClass+' .widget-controls :last').removeClass(config.maximizeClass).addClass(config.minimizeClass);
            $widget.find('.'+config.widgetContentClass).show();
        };

        var remove = function(widget) {
            _getElementAsJquery(widget).remove();
        };
        
        return {

            init : function(additionalConfig){
                openaksess.common.debug("openaksess.admin.widgetmanager.init(): Starting widget manager."); 

                if (additionalConfig && typeof(additionalConfig) == 'object') {
                    openaksess.common.debug("openaksess.admin.widgetmanager.init(): Extending default config with user supplied config.");
                    $.extend(config, additionalConfig);
                }
    
                var $columns = $('.'+config.widgetContainerClass, config.context);
                $columns.sortable({
                    connectWith: config.context + ' .'+config.widgetContainerClass
                });
    
                var $widgets = $('.'+config.widgetClass, config.context);
                $widgets.addClass("ui-widget ui-widget-content ui-helper-clearfix")
                        .find('.'+config.widgetHeaderClass)
                        .addClass("ui-widget-header")
                        .append('<span class="widget-controls"><span class="'+config.closeClass+'"></span><span class="'+config.minimizeClass+'"></span></span>');
    
                //Attach click listeners to controls (minimize/maximize/close)
                $widgets.find(".widget-header .widget-controls").live('click', function(e) {
                    var $target = $(e.target);
                    openaksess.common.debug("widgetmanager: Control click received. target: " + config.minimizeClass);
                    var $widget = $(this).parents(".widget:first");
    
                    if ($target.hasClass(config.minimizeClass)) {
                        openaksess.common.debug("widgetmanager: Click is minimize.");
                        minimize($widget);
                    }
                    else if ($target.hasClass(config.maximizeClass)) {
                        openaksess.common.debug("widgetmanager: Click is maximize.");
                        maximize($widget);
                    }
                    else if($target.hasClass(config.closeClass)) {
                        openaksess.common.debug("widgetmanager: Click is close.");
                        remove($widget);
                    }
    
                });
    
                $columns.disableSelection();
            },

            minimize : minimize,
            maximize : maximize,
            remove : remove
            
        };




    }(),

    createWidgets: function(){
        $(".infoslider").infoslider();
    }

};


/**
* Handles opening and closing of the support information container,
* typically located below the status bar and the content filters.
*/
$.widget("ui.infoslider", {

    options: {
        sliderCssClass: "infoslider",
        width : '100%',
        open : false,
        opener: undefined,
        floated: true,
        cssClasses : '',
        resizable: false
    },

    _init: function(){
        openaksess.common.debug("Widget.infoslider._init(): Slider created.");
        this.element.width(this.options.width);
    },

    /**
     * Opens the infoslider if it's hidden.
     * If the infoslider is open and toogle() is called by the same element as the one opening it, the slider is closed.
     * If the infoslider is open and toogle() is called by a different element than the one opening it, the content is replaced
     * and the slider is kept open.
     *
     * Example: If there exists an infoslider within a div with id "Navigator", this parameter will be "#Navigator".
     * @param opener - The object which triggered toggle of slider
     * @param content - The content to display in the info slider.
     */
    toggle: function(opener, content){
        if (!this.options.open) {
            this._openSlider(content);
        } else {
            if (opener == this.options.opener) {
                this._closeSlider();
            } else {
                this._setContent(content);
            }
        }
        this.options.opener = opener;
    },

    _openSlider: function(content){
        openaksess.common.debug("Widget.infoslider._openSlider(): Opening");
        this._setContent(content);
        this.element.slideDown();
        this.options.open = true;
    },

    _setContent: function(content){
        openaksess.common.debug("Widget.infoslider._setContent(): Setting content");
        if (content instanceof jQuery) {
            this.options.parent = content.parent();
            this.options.originalcontent = content.wrap("<div>").parent().html();
            content = content.detach();
            content.show();
        } else {
            content = $(content);
        }


        this.element
                .append(content)
                .wrapInner('<div class="slidercontent"/>')
                .removeClass()
                .addClass(this.options.sliderCssClass + " " + this.options.cssClasses)
                .css({height: 'auto', top: 'auto'});
        this._applyCloser();
        this._setFloat();
        this._setResizability();
    },

    _closeSlider: function(){
        openaksess.common.debug("Widget.infoslider._closeSlider(): Closing");
        this.element.slideUp();
        this._reset();
    },

    _setFloat: function() {
        if (this.options.floated) {
            openaksess.common.debug("Widget.infoslider._setFloat(): Setting slider to floated");
            this.element.css('position', 'absolute');
        } else {
            openaksess.common.debug("Widget.infoslider._setFloat(): Setting slider to relative");
            this.element.css('position', 'relative');
        }
    },

    _setResizability: function(){
        var slider = this.element;
        slider.resizable("destroy");
        if (this.options.resizable) {

            slider.resizable({
                handles: 's',
                start: function(){
                    if (typeof openaksess.navigate.navigatorResizeOnStart == 'function') {
                        var $iframe = $(this).find("iframe");
                        //Apply an overlay to the infoslider if it contains an iframe. Iframes mess up the resizing...
                        if ($iframe.size() > 0) {
                            openaksess.common.debug("InfoSlider.toggle(): Resize start. Iframe found. Adding overlay");
                            var height = slider.height();
                            var width = slider.width();
                            var overlay = $("<div/>").css({"position": "absolute", "height": height + "px", "width": width + "px", "background": "#ffffff", "opacity": "0"}).attr("id", "InforSlider_overlay");
                            $iframe.before(overlay);
                        }
                        openaksess.navigate.navigatorResizeOnStart();
                    }
                },
                stop: function() {
                    if (typeof openaksess.navigate.navigatorResizeOnStop == 'function') {
                        $("#InforSlider_overlay").remove();
                        openaksess.navigate.navigatorResizeOnStop();
                    }
                },
                resize: function() {
                    var $iframe = $(this).find("iframe");
                    if ($iframe.size() > 0) {
                        $iframe.height(slider.height());
                    }
                    if (typeof openaksess.navigate.navigatorResizeOnResize == 'function') {
                        openaksess.navigate.navigatorResizeOnResize();
                    }
                }
            });
        }
    },

    _reset: function(){
        openaksess.common.debug("Widget.infoslider._reset(): Resetting slider.");
        this.element.html("").removeClass().addClass(this.options.sliderCssClass);
        if (this.options.parent && this.options.originalcontent) {
            this.options.parent.append(this.options.originalcontent);
        }
        this.options.open = false;
        this.options.floated = true;
        this.options.parent = undefined;
        this.options.originalcontent = undefined;
        this.element.resizable("destroy");
    },

    _applyCloser: function(){
        var slider = this;
        var closer = $('<div class="close"><span>&nbsp;</span></div>').click(function(){
            slider._closeSlider();
        });
        this.element.append(closer);
    }

});










