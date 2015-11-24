window.org_bluemix_challenge_ui_components_ExternalLinkTarget = function() {

    var me = this;
    me.scanLinks = function() {
        var el = me.getElement(me.getParentId());
        console.log("================ parent ", el);
        if (el) {
            var links = el.querySelectorAll("a[href]");
            console.log("================ search links", links);
            for (var idx = 0; idx < links.length; idx++) {
                console.log("================ link target", links[idx].target);
               if (!links[idx].target) {
                 console.log("================ changed link target");
                 links[idx].target = '_info';
              }
            }
        }
    };
    me.scanLinks();
};