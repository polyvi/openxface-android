var xDebug = (function() {
    function xDebug(){
        this.host = null;
        this.port = null;
        this.client = null;
        this.log = function(){};
        this.reload = function(){};
    }

    return new xDebug();
})();
